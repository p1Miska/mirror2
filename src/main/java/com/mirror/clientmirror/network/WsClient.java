package com.mirror.clientmirror.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mirror.clientmirror.ClientMirrorMod;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Обёртка над WebSocketClient.
 *
 * ВАЖНО: колбэки onMessage/onOpen/onClose вызываются в сетевом потоке
 * Java-WebSocket, а не в потоке клиента Minecraft. Все входящие сообщения
 * кладём в очередь и разбираем на клиентском тике (MessageDispatcher.pump()),
 * чтобы не трогать World/Entity вне главного потока — это гарантированно
 * приводит к ConcurrentModificationException и краш-репортам в ваниле.
 */
public class WsClient {

    private static WsClient instance;

    private WebSocketClient client;
    private volatile boolean shouldRun = false;
    private long lastReconnectAttempt = 0L;
    private static final long RECONNECT_INTERVAL_MS = 3000L;

    public final LinkedBlockingQueue<JsonObject> inboundQueue = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<JsonObject> outboundQueue = new LinkedBlockingQueue<>();

    public static WsClient get() {
        if (instance == null) instance = new WsClient();
        return instance;
    }

    public void start(String url) {
        shouldRun = true;
        connect(url);
    }

    private void connect(String url) {
        try {
            URI uri = new URI(url);
            client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    ClientMirrorMod.LOGGER.info("[clientmirror] WS подключен: " + url);
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JsonObject obj = new JsonParser().parse(message).getAsJsonObject();
                        inboundQueue.add(obj);
                    } catch (Exception e) {
                        ClientMirrorMod.LOGGER.warn("[clientmirror] Некорректный JSON от сервера: " + e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    ClientMirrorMod.LOGGER.warn("[clientmirror] WS отключен (" + reason + "), переподключение...");
                }

                @Override
                public void onError(Exception ex) {
                    ClientMirrorMod.LOGGER.error("[clientmirror] WS ошибка: " + ex.getMessage());
                }
            };
            client.setConnectionLostTimeout(15);
            client.connect(); // асинхронно
        } catch (Exception e) {
            ClientMirrorMod.LOGGER.error("[clientmirror] Не удалось создать WS-подключение: " + e.getMessage());
        }
    }

    /** Вызывать раз в тик из главного потока — держит соединение живым и шлёт исходящую очередь. */
    public void tick(String url) {
        if (!shouldRun) return;

        if (client == null || client.isClosed() || client.isClosing()) {
            long now = System.currentTimeMillis();
            if (now - lastReconnectAttempt > RECONNECT_INTERVAL_MS) {
                lastReconnectAttempt = now;
                connect(url);
            }
            return;
        }

        JsonObject toSend;
        while ((toSend = outboundQueue.poll()) != null) {
            if (client.isOpen()) {
                client.send(toSend.toString());
            }
        }
    }

    public void send(JsonObject obj) {
        outboundQueue.add(obj);
    }

    public void stop() {
        shouldRun = false;
        if (client != null) {
            client.close();
        }
    }

    public boolean isConnected() {
        return client != null && client.isOpen();
    }
}
