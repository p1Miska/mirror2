package com.mirror.clientmirror.gui;

import com.mirror.clientmirror.network.OutboundSender;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import java.io.IOException;

/**
 * Настоящий ванильный GuiFurnace. RemoteInventory.getField() пока всегда
 * возвращает 0 (сервер не шлёт burn/cook progress) — слоты (сырьё/топливо/
 * результат) синхронизируются нормально, а огонёк и стрелка прогресса будут
 * статичными до тех пор, пока server.js не начнёт слать поля 0-3
 * (burnTime, currentItemBurnTime, cookTime, totalCookTime).
 */
public class RemoteGuiFurnace extends GuiFurnace {

    public RemoteGuiFurnace(InventoryPlayer playerInv, RemoteInventory furnaceInv) {
        super(playerInv, furnaceInv);
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
