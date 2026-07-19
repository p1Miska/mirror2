package com.mirror.clientmirror.network;

import com.google.gson.JsonObject;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Сборка и отправка исходящих сообщений в WS в формате, который понимает
 * server.js (handleClientMessage): input/look/chat/dig/attack/interact/
 * openInventory/windowClick/closeWindow/selectHotbar.
 */
public final class OutboundSender {

    public static void sendDig(BlockPos pos) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "dig");
        msg.addProperty("x", pos.getX());
        msg.addProperty("y", pos.getY());
        msg.addProperty("z", pos.getZ());
        WsClient.get().send(msg);
    }

    public static void sendInteract(BlockPos pos, EnumFacing face) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "interact");
        msg.addProperty("x", pos.getX());
        msg.addProperty("y", pos.getY());
        msg.addProperty("z", pos.getZ());
        msg.addProperty("nx", face.getFrontOffsetX());
        msg.addProperty("ny", face.getFrontOffsetY());
        msg.addProperty("nz", face.getFrontOffsetZ());
        WsClient.get().send(msg);
    }

    public static void sendAttack(String username) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "attack");
        msg.addProperty("username", username);
        WsClient.get().send(msg);
    }

    public static void sendChat(String text) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "chat");
        msg.addProperty("text", text);
        WsClient.get().send(msg);
    }

    public static void sendInput(boolean forward, boolean back, boolean left, boolean right,
                                  boolean jump, boolean sneak, boolean sprint) {
        JsonObject keys = new JsonObject();
        keys.addProperty("forward", forward);
        keys.addProperty("back", back);
        keys.addProperty("left", left);
        keys.addProperty("right", right);
        keys.addProperty("jump", jump);
        keys.addProperty("sneak", sneak);
        keys.addProperty("sprint", sprint);

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "input");
        msg.add("keys", keys);
        WsClient.get().send(msg);
    }

    /** Точка в мировых координатах, на которую смотрит камера (см. комментарий в server.js про конвенцию yaw). */
    public static void sendLook(double x, double y, double z) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "look");
        msg.addProperty("x", x);
        msg.addProperty("y", y);
        msg.addProperty("z", z);
        WsClient.get().send(msg);
    }

    public static void sendOpenInventory() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "openInventory");
        WsClient.get().send(msg);
    }

    public static void sendCloseWindow() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "closeWindow");
        WsClient.get().send(msg);
    }

    public static void sendSelectHotbar(int slot) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "selectHotbar");
        msg.addProperty("slot", slot);
        WsClient.get().send(msg);
    }

    public static void sendWindowClick(int slot, int button, boolean shift) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "windowClick");
        msg.addProperty("slot", slot);
        msg.addProperty("button", button);
        msg.addProperty("shift", shift);
        WsClient.get().send(msg);
    }

    private OutboundSender() {}
}
