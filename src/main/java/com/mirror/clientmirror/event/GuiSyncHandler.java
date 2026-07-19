package com.mirror.clientmirror.event;

import com.mirror.clientmirror.network.OutboundSender;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Требование п.6 (частично): держим сервер в курсе, что игрок открыл/закрыл
 * инвентарь, и синхронизируем выбранный слот хотбара (колесо мыши/цифры 1-9).
 * Полноценный "read-through" GuiContainer с содержимым чужого сундука — см.
 * gui/RemoteWindowManager (заготовка, дорабатывается на реальном примере payload).
 */
public class GuiSyncHandler {

    private boolean wasInventoryOpen = false;
    private int lastHotbarSlot = -1;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        GuiScreen gui = event.getGui();
        boolean opening = gui instanceof GuiInventory;

        if (opening && !wasInventoryOpen) {
            OutboundSender.sendOpenInventory();
        } else if (gui == null && wasInventoryOpen) {
            OutboundSender.sendCloseWindow();
        }
        wasInventoryOpen = opening;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        net.minecraft.client.entity.EntityPlayerSP player = net.minecraft.client.Minecraft.getMinecraft().player;
        if (player == null) return;

        int slot = player.inventory.currentItem;
        if (slot != lastHotbarSlot) {
            lastHotbarSlot = slot;
            OutboundSender.sendSelectHotbar(slot);
        }
    }
}
