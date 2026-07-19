package com.mirror.clientmirror.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Универсальный контейнер под ЛЮБОЙ верхний инвентарь (сундук 27, двойной
 * сундук 54, печь/верстак и т.д.) — раскладка top-слотов в сетку по 9 в ряд,
 * снизу стандартные 3x9 + хотбар как в обычном инвентаре игрока.
 *
 * ВАЖНО: этот Container не участвует в реальном перемещении предметов —
 * все клики перехватываются в GuiRemoteContainer.mouseClicked и уходят как
 * windowClick в WS напрямую, минуя стандартный Container.slotClick. Слоты
 * здесь только для отрисовки текущего состояния (RemoteInventory).
 */
public class RemoteContainer extends Container {

    private final int topSize;

    public RemoteContainer(RemoteInventory topInventory, InventoryPlayer playerInv) {
        this.topSize = topInventory.getSizeInventory();

        int rows = (int) Math.ceil(topSize / 9.0);
        int top = 18;
        for (int i = 0; i < topSize; i++) {
            int col = i % 9;
            int row = i / 9;
            this.addSlotToContainer(new Slot(topInventory, i, 8 + col * 18, top + row * 18));
        }

        int playerInvTop = top + rows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, playerInvTop + row * 18));
            }
        }
        int hotbarY = playerInvTop + 3 * 18 + 4;
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }
    }

    public int getTopSize() { return topSize; }

    @Override
    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(net.minecraft.entity.player.EntityPlayer playerIn, int index) {
        return ItemStack.EMPTY; // shift-click перенос отключён — не имеем права решать это локально
    }
}
