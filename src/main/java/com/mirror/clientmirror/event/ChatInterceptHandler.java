package com.mirror.clientmirror.event;

import com.mirror.clientmirror.network.OutboundSender;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Игрок печатает в обычный чат Minecraft (T / Enter) — вместо того чтобы это
 * ушло в никуда локальному SP-серверу, отменяем и пересылаем в WS (bot.chat на бэкенде).
 * Входящие сообщения ("chat" от сервера) выводятся отдельно в MessageDispatcher.onChat.
 */
public class ChatInterceptHandler {

    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        String msg = event.getMessage();
        if (msg == null || msg.isEmpty()) return;

        // Оставляем локальные клиентские команды (например, будущие "/mirror ...")
        // проходить как обычно, если понадобится расширить мод собственными командами.
        event.setCanceled(true);
        OutboundSender.sendChat(msg);
    }
}
