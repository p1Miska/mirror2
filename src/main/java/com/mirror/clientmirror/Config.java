package com.mirror.clientmirror;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Настройки мода. Хранится в config/clientmirror.cfg.
 */
public final class Config {

    public static String wsUrl = "ws://localhost:8081";
    // Насколько сильно доверяем локальной физике до "мягкой" коррекции к серверной позиции.
    public static double softCorrectionThreshold = 4.0; // блоков расхождения -> начинаем подтягивать
    public static double hardTeleportThreshold = 16.0;  // блоков расхождения -> считаем это телепортом/сменой сервера
    public static boolean fillUnknownWithBarrier = true; // чем "затыкать" непереданные соседние блоки
    public static int voidFillRadius = 10; // радиус проверки/подстилки барьера под ноги игрока

    private static Configuration cfg;

    public static void load(File file) {
        cfg = new Configuration(file);
        cfg.load();

        wsUrl = cfg.getString("wsUrl", "network", wsUrl,
                "Адрес WebSocket-моста (server.js)");
        softCorrectionThreshold = cfg.getFloat("softCorrectionThreshold", "physics",
                (float) softCorrectionThreshold, 0.5f, 32f,
                "Расхождение локальной и серверной позиции (блоков), с которого начинаем плавно подтягивать игрока");
        hardTeleportThreshold = cfg.getFloat("hardTeleportThreshold", "physics",
                (float) hardTeleportThreshold, 4f, 128f,
                "Расхождение позиции, при котором считаем что произошёл телепорт/смена сервера и просто переносим игрока");
        fillUnknownWithBarrier = cfg.getBoolean("fillUnknownWithBarrier", "world", fillUnknownWithBarrier,
                "Заполнять непереданные сервером блоки барьером (коллизия есть, невидим), чтобы не проваливаться в пустоту");
        voidFillRadius = cfg.getInt("voidFillRadius", "world", voidFillRadius, 2, 32,
                "Радиус проверки/подстилки барьера под ногами игрока");

        if (cfg.hasChanged()) cfg.save();
    }
}
