package com.mirror.clientmirror.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mirror.clientmirror.Config;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

/**
 * Применяет "blocks" (полный видимый набор вокруг игрока) и "blockChange"
 * (точечные изменения) к локальному клиентскому миру.
 *
 * Сервер шлёт только "видимые" (с открытой гранью) блоки в радиусе — то есть
 * это не полный снимок чанка. Чтобы не проваливаться в пустоту там, где
 * сервер просто не прислал данные (внутренности построек, дальний радиус),
 * держим известный "футпринт" последнего снапшота и всё, что раньше было
 * известно, но пропало из нового snapshot и не пришло явным blockChange,
 * НЕ трогаем — трогаем только то, что реально указано.
 */
public class WorldSyncHandler {

    // Все координаты, которые мы когда-либо сами устанавливали — чтобы отличать
    // "наши" блоки от блоков плоского мира под ногами, и не затирать их случайно.
    private final Set<BlockPos> managedPositions = new HashSet<>();

    private static final WorldSyncHandler INSTANCE = new WorldSyncHandler();
    public static WorldSyncHandler get() { return INSTANCE; }

    /** {type:"blocks", blocks:[{x,y,z,name}, ...]} — полный видимый набор вокруг игрока сейчас. */
    public void applyBlocksSnapshot(JsonObject msg) {
        World world = Minecraft.getMinecraft().world;
        if (world == null) return;

        JsonArray arr = msg.getAsJsonArray("blocks");
        if (arr == null) return;

        Set<BlockPos> newPositions = new HashSet<>();

        for (int i = 0; i < arr.size(); i++) {
            JsonObject b = arr.get(i).getAsJsonObject();
            int x = b.get("x").getAsInt();
            int y = b.get("y").getAsInt();
            int z = b.get("z").getAsInt();
            String name = b.get("name").getAsString();

            if (y < 0 || y > 255) continue; // 1.12.2 предел высоты

            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = BlockMapper.resolve(name);
            world.setBlockState(pos, state, 3);
            newPositions.add(pos);
        }

        // Блоки, которые раньше считались "нашими", но пропали из нового snapshot
        // (то есть сервер их больше не видит как видимые/существующие рядом с игроком) —
        // убираем, если только это не помечено фильтром пустоты (grass_path и т.п. остаётся).
        managedPositions.removeIf(pos -> {
            if (!newPositions.contains(pos)) {
                world.setBlockToAir(pos);
                return true;
            }
            return false;
        });
        managedPositions.addAll(newPositions);
    }

    /** {type:"blockChange", x,y,z, name?, remove?} — точечное изменение. */
    public void applyBlockChange(JsonObject msg) {
        World world = Minecraft.getMinecraft().world;
        if (world == null) return;

        int x = msg.get("x").getAsInt();
        int y = msg.get("y").getAsInt();
        int z = msg.get("z").getAsInt();
        BlockPos pos = new BlockPos(x, y, z);

        if (msg.has("remove") && msg.get("remove").getAsBoolean()) {
            world.setBlockToAir(pos);
            managedPositions.remove(pos);
            return;
        }

        if (msg.has("name")) {
            IBlockState state = BlockMapper.resolve(msg.get("name").getAsString());
            world.setBlockState(pos, state, 3);
            managedPositions.add(pos);
        }
    }

    /**
     * Заполняет барьером границы известной области, чтобы игрок не проваливался
     * в "нерендеренную" пустоту за пределами присланного сервером радиуса блоков.
     * Вызывается умеренно (не каждый тик) вокруг текущей позиции игрока.
     */
    public void fillVoidFloorIfNeeded(BlockPos center, int radius) {
        if (!Config.fillUnknownWithBarrier) return;
        World world = Minecraft.getMinecraft().world;
        if (world == null) return;

        int y = center.getY() - 2;
        if (y < 0) return;
        for (int dx = -radius; dx <= radius; dx += radius) {
            for (int dz = -radius; dz <= radius; dz += radius) {
                BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                if (world.isAirBlock(pos) && !managedPositions.contains(pos)) {
                    world.setBlockState(pos, Blocks.BARRIER.getDefaultState(), 2);
                    managedPositions.add(pos);
                }
            }
        }
    }

    /** Полный сброс — вызывается при телепорте/смене сервера. */
    public void reset() {
        World world = Minecraft.getMinecraft().world;
        if (world != null) {
            for (BlockPos pos : managedPositions) {
                world.setBlockToAir(pos);
            }
        }
        managedPositions.clear();
    }
}
