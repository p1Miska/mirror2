package com.mirror.clientmirror.gui;

import com.mirror.clientmirror.network.OutboundSender;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import java.io.IOException;

/**
 * Пока без привязки к конкретной ванильной текстуре сундука/печи/верстака —
 * рисуем нейтральную нативную сетку (одинаковую для всех типов контейнеров),
 * чтобы не гадать раскладку без реального примера "window"-payload с сервера.
 * Функционально работает для любого размера top-инвентаря; "покраска" под
 * конкретный тип (chest.png/furnace.png/...) — следующий шаг, когда придут
 * дампы (см. предыдущее сообщение).
 */
public class GuiRemoteContainer extends GuiContainer {

    private final String title;

    public GuiRemoteContainer(RemoteContainer container, InventoryPlayer playerInv, String title) {
        super(container);
        this.title = title;
        int rows = (int) Math.ceil(container.getTopSize() / 9.0);
        this.ySize = 18 + rows * 18 + 14 + 3 * 18 + 4 + 18 + 6;
        this.xSize = 176;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.disableLighting();
        GlStateManager.color(1f, 1f, 1f, 1f);
        // Нейтральная панель вместо текстуры — простые заливки, без внешних assets.
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xC0202020);
        for (Slot slot : this.inventorySlots.inventorySlots) {
            int x = guiLeft + slot.xPos - 1;
            int y = guiTop + slot.yPos - 1;
            drawRect(x, y, x + 18, y + 18, 0xFF8B8B8B);
            drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(title, 8, 6, 0xFFFFFF);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Полностью своя обработка вместо super — не даём ванили попытаться
        // реально сдвинуть предметы в RemoteInventory (он и так read-only,
        // но так чище и явно видно намерение: "клик = команда на сервер").
        Slot clicked = getSlotAt(mouseX, mouseY);
        if (clicked != null) {
            boolean shift = GuiScreen.isShiftKeyDown();
            OutboundSender.sendWindowClick(clicked.slotNumber, mouseButton, shift);
        }
    }

    private Slot getSlotAt(int mouseX, int mouseY) {
        for (Slot slot : this.inventorySlots.inventorySlots) {
            int x = guiLeft + slot.xPos;
            int y = guiTop + slot.yPos;
            if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        OutboundSender.sendCloseWindow();
    }
}
