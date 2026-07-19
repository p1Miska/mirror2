package com.mirror.clientmirror.gui;

import com.mirror.clientmirror.network.OutboundSender;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import java.io.IOException;

/** Как и у печки: слоты живые, полоска варки/индикатор топлива статичны (getField()==0). */
public class RemoteGuiBrewingStand extends GuiBrewingStand {

    public RemoteGuiBrewingStand(InventoryPlayer playerInv, RemoteInventory brewingInv) {
        super(playerInv, brewingInv);
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
