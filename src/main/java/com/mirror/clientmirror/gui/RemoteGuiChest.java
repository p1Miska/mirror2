package com.mirror.clientmirror.gui;

import com.mirror.clientmirror.network.OutboundSender;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import java.io.IOException;

/**
 * Настоящий ванильный GuiChest (та же текстура/раскладка, что видит игрок на
 * реальном сервере) поверх нашего read-only RemoteInventory. GuiChest сам
 * генерит нужное количество рядов из inventory.getSizeInventory()/9, поэтому
 * подходит и для одиночного (27), и для двойного сундука (54), и для шалкер-
 * бокса (27) без изменений.
 *
 * mouseClicked полностью переопределён: вместо стандартного Container.slotClick
 * (который в любом случае no-op на RemoteInventory) шлём windowClick в WS —
 * реальное изменение вернётся следующим "window" от сервера.
 */
public class RemoteGuiChest extends GuiChest {

    public RemoteGuiChest(InventoryPlayer playerInv, RemoteInventory chestInv) {
        super(playerInv, chestInv);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        Slot clicked = findSlotAt(mouseX, mouseY);
        if (clicked != null) {
            OutboundSender.sendWindowClick(clicked.slotNumber, mouseButton, GuiScreen.isShiftKeyDown());
        }
    }

    private Slot findSlotAt(int mouseX, int mouseY) {
        for (Slot slot : this.inventorySlots.inventorySlots) {
            int x = guiLeft + slot.xPos;
            int y = guiTop + slot.yPos;
            if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) return slot;
        }
        return null;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        OutboundSender.sendCloseWindow();
    }
}
