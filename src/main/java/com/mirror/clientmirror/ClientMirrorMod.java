package com.mirror.clientmirror;

import com.mirror.clientmirror.event.BlockInteractionHandler;
import com.mirror.clientmirror.event.ChatInterceptHandler;
import com.mirror.clientmirror.event.ClientTickHandler;
import com.mirror.clientmirror.event.GuiSyncHandler;
import com.mirror.clientmirror.network.WsClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = "clientmirror", name = "Client Mirror", version = "1.0.0", clientSideOnly = true)
public class ClientMirrorMod {

    public static final Logger LOGGER = LogManager.getLogger("ClientMirror");

    private java.io.File configFile;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
        Config.load(configFile);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
        MinecraftForge.EVENT_BUS.register(new BlockInteractionHandler());
        MinecraftForge.EVENT_BUS.register(new ChatInterceptHandler());
        MinecraftForge.EVENT_BUS.register(new GuiSyncHandler());
    }

    // Подключаемся, когда игрок реально появился в локальном мире (плоский/void SP-мир),
    // и отключаемся, когда мир выгружается — иначе WS будет пытаться жить между мирами.
    @SubscribeEvent
    public void onWorldJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        LOGGER.info("[clientmirror] Локальный мир загружен, подключаюсь к мосту: " + Config.wsUrl);
        WsClient.get().start(Config.wsUrl);
    }

    @SubscribeEvent
    public void onWorldLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        LOGGER.info("[clientmirror] Выход из мира, закрываю WS.");
        WsClient.get().stop();
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals("clientmirror") && configFile != null) {
            Config.load(configFile);
        }
    }
}
