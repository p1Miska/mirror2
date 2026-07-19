package com.mirror.clientmirror.gui;

import com.mirror.clientmirror.item.ItemMapper;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * "Тупой" контейнер-хранилище только для отображения. Никакой собственной
 * логики (стакание/лимиты/крафт) — содержимое целиком перезаписывается из
 * RemoteWindowManager при каждом "window"-сообщении сервера. Клики по слотам
 * НЕ меняют этот инвентарь напрямую (см. GuiRemoteContainer.mouseClicked) —
 * они только шлют windowClick в WS; реальное изменение придёт следующим
 * "window" от сервера, который и обновит содержимое здесь.
 */
public class RemoteInventory implements IInventory {

    private NonNullList<ItemStack> stacks = NonNullList.create();
    private String title = "";

    public void setContentsFromServer(String title, java.util.List<RemoteWindowManager.SlotItem> items) {
        this.title = title;
        this.stacks = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            RemoteWindowManager.SlotItem it = items.get(i);
            this.stacks.set(i, it == null ? ItemStack.EMPTY : ItemMapper.resolve(it.name, it.count));
        }
    }

    @Override public int getSizeInventory() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStackInSlot(int index) { return index >= 0 && index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY; }

    // Локальное "изъятие" не поддерживаем — источник истины сервер, локально ничего не двигаем.
    @Override public ItemStack decrStackSize(int index, int count) { return ItemStack.EMPTY; }
    @Override public ItemStack removeStackFromSlot(int index) { return ItemStack.EMPTY; }
    @Override public void setInventorySlotContents(int index, ItemStack stack) { /* no-op: сервер авторитетен */ }

    @Override public int getInventoryStackLimit() { return 64; }
    @Override public void markDirty() { }
    @Override public boolean isUsableByPlayer(net.minecraft.entity.player.EntityPlayer player) { return true; }
    @Override public void openInventory(net.minecraft.entity.player.EntityPlayer player) { }
    @Override public void closeInventory(net.minecraft.entity.player.EntityPlayer player) { }
    @Override public boolean isItemValidForSlot(int index, ItemStack stack) { return false; }

    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) { }
    @Override public int getFieldCount() { return 0; }
    @Override public void clear() { stacks.clear(); }

    @Override public String getName() { return title; }
    @Override public boolean hasCustomName() { return true; }
    @Override public ITextComponent getDisplayName() { return new TextComponentString(title); }
}
