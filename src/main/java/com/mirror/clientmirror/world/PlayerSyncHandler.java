package com.mirror.clientmirror.world;

import com.google.gson.JsonObject;
import com.mirror.clientmirror.Config;
import com.mirror.clientmirror.entity.RemotePlayerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.Vec3d;

/**
 * "Локальная позиция считается основной" (п.5 требований) — мы НЕ телепортируем
 * игрока на каждый server state. Вместо этого:
 *  - если расхождение маленькое: ничего не делаем (или в будущем — мягкий вектор коррекции);
 *  - если расхождение среднее (softCorrectionThreshold): начинаем плавно подтягивать;
 *  - если расхождение огромное (hardTeleportThreshold): считаем это настоящим
 *    телепортом/сменой сервера/BungeeCord-переходом и жёстко переносим игрока,
 *    сбрасывая известный мир (см. WorldSyncHandler.reset()/RemotePlayerManager.reset()
 *    также вызываются из "status: spawned" — этот путь для случая, когда сервер прислал
 *    просто "state" со скачком позиции без промежуточного status-события).
 */
public class PlayerSyncHandler {

    private static final PlayerSyncHandler INSTANCE = new PlayerSyncHandler();
    public static PlayerSyncHandler get() { return INSTANCE; }

    private Vec3d lastServerPos = null;

    public void applyState(JsonObject msg) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || !msg.has("position")) return;

        JsonObject pos = msg.getAsJsonObject("position");
        Vec3d serverPos = new Vec3d(pos.get("x").getAsDouble(), pos.get("y").getAsDouble(), pos.get("z").getAsDouble());

        if (lastServerPos != null) {
            double serverJump = serverPos.distanceTo(lastServerPos);
            double localDrift = serverPos.distanceTo(player.getPositionVector());

            if (serverJump > Config.hardTeleportThreshold) {
                // Сервер сам резко переместил бота (телепорт/портал/смена сервера) —
                // синхронизируем клиента без перезапуска мира.
                hardTeleport(player, serverPos);
            } else if (localDrift > Config.hardTeleportThreshold) {
                // Разошлись слишком сильно (например, локальная физика провалилась
                // в незагруженную область) — тоже жёстко подтягиваем.
                hardTeleport(player, serverPos);
            } else if (localDrift > Config.softCorrectionThreshold) {
                softCorrect(player, serverPos);
            }
        }

        lastServerPos = serverPos;
    }

    private void hardTeleport(EntityPlayerSP player, Vec3d target) {
        player.setPositionAndUpdate(target.x, target.y, target.z);
        WorldSyncHandler.get().reset();
        RemotePlayerManager.get().reset();
    }

    private void softCorrect(EntityPlayerSP player, Vec3d target) {
        // Плавно подтягиваем 10% расхождения за тик, а не телепортируем —
        // локальная физика остаётся "главной" (п.5), сервер — мягкий ориентир.
        double nx = player.posX + (target.x - player.posX) * 0.1;
        double ny = player.posY + (target.y - player.posY) * 0.1;
        double nz = player.posZ + (target.z - player.posZ) * 0.1;
        player.setPosition(nx, ny, nz);
    }

    public void applyHealth(JsonObject msg) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;
        if (msg.has("health")) player.setHealth(msg.get("health").getAsFloat());
        if (msg.has("food")) player.getFoodStats().setFoodLevel(msg.get("food").getAsInt());
    }

    public void reset() {
        lastServerPos = null;
    }
}
