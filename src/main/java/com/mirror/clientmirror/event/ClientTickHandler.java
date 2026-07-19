package com.mirror.clientmirror.event;

import com.mirror.clientmirror.Config;
import com.mirror.clientmirror.network.MessageDispatcher;
import com.mirror.clientmirror.network.OutboundSender;
import com.mirror.clientmirror.network.WsClient;
import com.mirror.clientmirror.world.WorldSyncHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Главный клиентский тик мода. Всё, что трогает World/Entity/сеть на нашей стороне,
 * идёт отсюда — единая точка синхронизации с главным потоком клиента.
 */
public class ClientTickHandler {

    private int tickCounter = 0;

    // Шлём look не каждый тик (20/сек) — незачем забивать канал, достаточно ~10/сек,
    // синхронно с тем как сервер шлёт "state" (см. STATE_TICK_MS=100 в server.js).
    private static final int LOOK_SEND_INTERVAL = 2;
    private static final int VOID_FILL_INTERVAL = 20; // раз в секунду

    // Последнее отправленное состояние клавиш — шлём "input" только при изменении,
    // чтобы не спамить WS 20 раз в секунду одинаковыми данными.
    private boolean lastForward, lastBack, lastLeft, lastRight, lastJump, lastSneak, lastSprint;
    private boolean firstSend = true;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        WsClient ws = WsClient.get();

        ws.tick(Config.wsUrl);
        MessageDispatcher.pump();

        if (mc.player == null || mc.world == null) return;

        tickCounter++;

        sendInputIfChanged(mc.player);

        if (tickCounter % LOOK_SEND_INTERVAL == 0) {
            sendLook(mc.player);
        }

        if (tickCounter % VOID_FILL_INTERVAL == 0) {
            WorldSyncHandler.get().fillVoidFloorIfNeeded(new BlockPos(mc.player), Config.voidFillRadius);
        }
    }

    private void sendInputIfChanged(EntityPlayerSP player) {
        GameSettings s = Minecraft.getMinecraft().gameSettings;
        boolean forward = s.keyBindForward.isKeyDown();
        boolean back = s.keyBindBack.isKeyDown();
        boolean left = s.keyBindLeft.isKeyDown();
        boolean right = s.keyBindRight.isKeyDown();
        boolean jump = s.keyBindJump.isKeyDown();
        boolean sneak = s.keyBindSneak.isKeyDown();
        boolean sprint = s.keyBindSprint.isKeyDown() || player.isSprinting();

        if (firstSend || forward != lastForward || back != lastBack || left != lastLeft
                || right != lastRight || jump != lastJump || sneak != lastSneak || sprint != lastSprint) {
            OutboundSender.sendInput(forward, back, left, right, jump, sneak, sprint);
            lastForward = forward; lastBack = back; lastLeft = left; lastRight = right;
            lastJump = jump; lastSneak = sneak; lastSprint = sprint;
            firstSend = false;
        }
    }

    private void sendLook(EntityPlayerSP player) {
        // Точка в мировых координатах вдоль направления взгляда — сервер (server.js)
        // сам конвертирует это в yaw/pitch через bot.lookAt(), см. комментарий там про
        // разные конвенции yaw. Берём точку на фиксированном расстоянии вдоль вектора взгляда.
        Vec3d look = player.getLook(1.0f);
        Vec3d eyes = player.getPositionEyes(1.0f);
        double dist = 4.0;
        Vec3d target = eyes.add(new Vec3d(look.x * dist, look.y * dist, look.z * dist));
        OutboundSender.sendLook(target.x, target.y, target.z);
    }
}
