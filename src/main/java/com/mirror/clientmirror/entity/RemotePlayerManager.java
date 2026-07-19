package com.mirror.clientmirror.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Держит по одной клиентской EntityOtherPlayerMP на каждого игрока из "state.players".
 * EntityOtherPlayerMP — тот же класс, которым ваниль рисует ЧУЖИХ игроков в обычном
 * многопользовательском мире, поэтому ник/модель/анимация работают "из коробки"
 * без написания собственного рендерера.
 *
 * Эти сущности существуют ТОЛЬКО на клиенте (никогда не добавляются на сервер/в
 * серверный тик), поэтому не ломают локальный мир и не мешают ванильной физике.
 */
public class RemotePlayerManager {

    private static final RemotePlayerManager INSTANCE = new RemotePlayerManager();
    public static RemotePlayerManager get() { return INSTANCE; }

    private final Map<String, EntityOtherPlayerMP> players = new HashMap<>();

    /** {type:"state", players:[{username,position:{x,y,z},yaw}, ...], ...} */
    public void applyState(JsonObject msg) {
        World world = Minecraft.getMinecraft().world;
        if (world == null) return;
        if (!msg.has("players")) return;

        JsonArray arr = msg.getAsJsonArray("players");
        java.util.Set<String> seen = new java.util.HashSet<>();

        for (int i = 0; i < arr.size(); i++) {
            JsonObject p = arr.get(i).getAsJsonObject();
            String username = p.get("username").getAsString();
            seen.add(username);

            JsonObject pos = p.getAsJsonObject("position");
            double x = pos.get("x").getAsDouble();
            double y = pos.get("y").getAsDouble();
            double z = pos.get("z").getAsDouble();
            float yaw = p.has("yaw") ? p.get("yaw").getAsFloat() : 0f;

            EntityOtherPlayerMP entity = players.get(username);
            if (entity == null) {
                entity = spawn(world, username, x, y, z, yaw);
                players.put(username, entity);
            } else {
                // Плавная интерполяция позиции между тиками state (~10/сек),
                // чтобы движение выглядело не рывками, а как в обычном MP.
                entity.setPositionAndRotation(x, y, z, yaw, entity.rotationPitch);
                entity.prevPosX = entity.posX;
                entity.prevPosY = entity.posY;
                entity.prevPosZ = entity.posZ;
            }
        }

        // Игроки, вышедшие из радиуса видимости / отключившиеся — убираем.
        players.entrySet().removeIf(entry -> {
            if (!seen.contains(entry.getKey())) {
                world.removeEntity(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private EntityOtherPlayerMP spawn(World world, String username, double x, double y, double z, float yaw) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("clientmirror:" + username).getBytes()), username);
        EntityOtherPlayerMP entity = new EntityOtherPlayerMP(world, profile);
        entity.setPositionAndRotation(x, y, z, yaw, 0);
        entity.setAlwaysRenderNameTag(true);
        entity.setCustomNameTag(username);
        world.spawnEntity(entity);
        return entity;
    }

    /** Полный сброс — вызывается при телепорте/смене сервера. */
    public void reset() {
        World world = Minecraft.getMinecraft().world;
        if (world != null) {
            for (EntityOtherPlayerMP e : players.values()) {
                world.removeEntity(e);
            }
        }
        players.clear();
    }
}
