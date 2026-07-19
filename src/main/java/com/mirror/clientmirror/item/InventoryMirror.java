package com.mirror.clientmirror.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Ключевая идея: вместо своего рендера хотбара/брони мы просто кладём
 * реальные ItemStack'и (замапленные через ItemMapper) в НАСТОЯЩИЙ инвентарь
 * локального EntityPlayerSP. Дальше всё рисует сама ваниль: HUD-хотбар внизу
 * экрана, броня на модели игрока (в т.ч. в GuiInventory и на других игроках
 * при рендере), тултипы при наведении — бесплатно, без единой строчки
 * собственного рендер-кода.
 *
 * Это read-only зеркало: если игрок потаскает предметы в СВОЁМ GuiInventory
 * руками, локальные стаки временно "разъедутся" с сервером — но при следующем
 * "hotbar"/"window" сообщении (сервер шлёт их на каждое изменение, см.
 * updateSlot listener в server.js) всё перезапишется обратно. Специально не
 * блокируем клики в собственном инвентаре — в отличие от чужих контейнеров,
 * тут это не критично и не стоит сложности кастомного Container.
 */
public final class InventoryMirror {

    /** {type:"hotbar", selected, hotbar:[...9], armor:{head,chest,legs,feet}} */
    public static void applyHotbar(JsonObject msg) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;

        JsonArray hotbar = msg.getAsJsonArray("hotbar");
        if (hotbar != null) {
            for (int i = 0; i < hotbar.size() && i < 9; i++) {
                player.inventory.mainInventory.set(i, toStack(hotbar.get(i)));
            }
        }
        if (msg.has("selected")) {
            player.inventory.currentItem = msg.get("selected").getAsInt();
        }
        if (msg.has("armor")) {
            JsonObject armor = msg.getAsJsonObject("armor");
            player.setItemStackToSlot(EntityEquipmentSlot.HEAD, toStack(armor.get("head")));
            player.setItemStackToSlot(EntityEquipmentSlot.CHEST, toStack(armor.get("chest")));
            player.setItemStackToSlot(EntityEquipmentSlot.LEGS, toStack(armor.get("legs")));
            player.setItemStackToSlot(EntityEquipmentSlot.FEET, toStack(armor.get("feet")));
        }
    }

    /**
     * {type:"window", title:"Инвентарь", slots:[46 элементов в порядке
     * протокола: 0=результат крафта,1-4=сетка крафта,5-8=броня,9-35=инвентарь,
     * 36-44=хотбар,45=офф-рука]} — приходит когда игрок открыл СВОЙ инвентарь (E).
     */
    public static void applyFullInventoryWindow(JsonObject msg) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;

        JsonArray slots = msg.getAsJsonArray("slots");
        if (slots == null) return;

        for (int i = 9; i <= 35 && i < slots.size(); i++) {
            player.inventory.mainInventory.set(i, toStack(slots.get(i)));
        }
        for (int i = 36; i <= 44 && i < slots.size(); i++) {
            player.inventory.mainInventory.set(i - 36, toStack(slots.get(i)));
        }
        if (slots.size() > 8) {
            player.setItemStackToSlot(EntityEquipmentSlot.HEAD, toStack(slots.get(5)));
            player.setItemStackToSlot(EntityEquipmentSlot.CHEST, toStack(slots.get(6)));
            player.setItemStackToSlot(EntityEquipmentSlot.LEGS, toStack(slots.get(7)));
            player.setItemStackToSlot(EntityEquipmentSlot.FEET, toStack(slots.get(8)));
        }
        if (slots.size() > 45) {
            player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, toStack(slots.get(45)));
        }
    }

    private static ItemStack toStack(com.google.gson.JsonElement el) {
        if (el == null || el.isJsonNull()) return ItemStack.EMPTY;
        JsonObject item = el.getAsJsonObject();
        String name = item.get("name").getAsString();
        int count = item.has("count") ? item.get("count").getAsInt() : 1;
        return ItemMapper.resolve(name, count);
    }

    private InventoryMirror() {}
}
