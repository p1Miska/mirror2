package com.mirror.clientmirror.network;

import com.google.gson.JsonObject;
import com.mirror.clientmirror.ClientMirrorMod;
import com.mirror.clientmirror.entity.RemotePlayerManager;
import com.mirror.clientmirror.gui.RemoteWindowManager;
import com.mirror.clientmirror.world.PlayerSyncHandler;
import com.mirror.clientmirror.world.WorldSyncHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.TextComponentString;

/**
 * Забирает сообщения из WsClient.inboundQueue и раскладывает их по обработчикам.
 * Вызывается из клиентского тик-обработчика — то есть ВСЕГДА в главном потоке.
 */
public final class MessageDispatcher {

    private static final int MAX_PER_TICK = 64; // защита от лавины blocks-снапшотов после реконнекта

    public static void pump() {
        WsClient ws = WsClient.get();
        int processed = 0;
        JsonObject msg;
        while (processed < MAX_PER_TICK && (msg = ws.inboundQueue.poll()) != null) {
            dispatch(msg);
            processed++;
        }
    }

    private static void dispatch(JsonObject msg) {
        if (!msg.has("type")) return;
        String type = msg.get("type").getAsString();

        try {
            switch (type) {
                case "state":
                    PlayerSyncHandler.get().applyState(msg);
                    RemotePlayerManager.get().applyState(msg);
                    break;
                case "blocks":
                    WorldSyncHandler.get().applyBlocksSnapshot(msg);
                    break;
                case "blockChange":
                    WorldSyncHandler.get().applyBlockChange(msg);
                    break;
                case "health":
                    PlayerSyncHandler.get().applyHealth(msg);
                    break;
                case "hotbar":
                    RemoteWindowManager.get().applyHotbar(msg);
                    com.mirror.clientmirror.item.InventoryMirror.applyHotbar(msg);
                    break;
                case "window": {
                    String title = msg.has("title") ? msg.get("title").getAsString() : "";
                    if ("Инвентарь".equals(title)) {
                        // Собственный инвентарь игрока — зеркалим в реальные ItemStack'и,
                        // дальше всё рисует ваниль (GuiInventory открыт через клавишу E).
                        com.mirror.clientmirror.item.InventoryMirror.applyFullInventoryWindow(msg);
                    } else {
                        // Чужой контейнер (сундук/печь/верстак/...) — свой read-through GUI.
                        RemoteWindowManager.get().applyWindow(msg);
                        com.mirror.clientmirror.gui.RemoteContainerManager.get()
                                .refreshOrOpen(title, RemoteWindowManager.get().getOpenWindowSlots());
                    }
                    break;
                }
                case "windowClose":
                    RemoteWindowManager.get().closeWindow();
                    com.mirror.clientmirror.gui.RemoteContainerManager.get().closeIfOpen();
                    break;
                case "chat":
                    onChat(msg);
                    break;
                case "loading":
                    // прогресс прогрузки чанков на сервере — можно вывести в actionbar; для MVP просто лог
                    break;
                case "status":
                    onStatus(msg);
                    break;
                default:
                    ClientMirrorMod.LOGGER.debug("[clientmirror] Неизвестный тип сообщения: " + type);
            }
        } catch (Exception e) {
            ClientMirrorMod.LOGGER.error("[clientmirror] Ошибка обработки сообщения type=" + type + ": " + e);
        }
    }

    private static void onChat(JsonObject msg) {
        String text = msg.has("text") ? msg.get("text").getAsString()
                : msg.has("message") ? msg.get("message").getAsString() : null;
        if (text == null) return;
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        if (p != null) {
            p.sendMessage(new TextComponentString(text));
        }
    }

    private static void onStatus(JsonObject msg) {
        String state = msg.has("state") ? msg.get("state").getAsString() : "";
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        switch (state) {
            case "spawned":
                // Новый спавн на бэкенде (в т.ч. смена сервера через Bungee/Velocity) —
                // сбрасываем локальный кэш мира и игроков, ждём свежие blocks/state.
                WorldSyncHandler.get().reset();
                RemotePlayerManager.get().reset();
                if (msg.has("position") && p != null) {
                    JsonObject pos = msg.getAsJsonObject("position");
                    p.setPositionAndUpdate(pos.get("x").getAsDouble(), pos.get("y").getAsDouble(), pos.get("z").getAsDouble());
                }
                if (p != null) p.sendMessage(new TextComponentString("[ClientMirror] Синхронизация с сервером..."));
                break;
            case "kicked":
            case "disconnected":
                if (p != null) p.sendMessage(new TextComponentString("[ClientMirror] Бот отключился от реального сервера: "
                        + (msg.has("reason") ? msg.get("reason").getAsString() : msg.has("message") ? msg.get("message").getAsString() : "")));
                break;
            case "error":
                if (p != null) p.sendMessage(new TextComponentString("[ClientMirror] Ошибка моста: "
                        + (msg.has("message") ? msg.get("message").getAsString() : "")));
                break;
            default:
                break;
        }
    }

    private MessageDispatcher() {}
}
