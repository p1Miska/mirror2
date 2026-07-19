package com.mirror.clientmirror.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Держит один открытый read-through GUI для ЧУЖОГО контейнера (не для
 * собственного инвентаря игрока — тот идёт через InventoryMirror + обычный
 * ванильный GuiInventory).
 *
 * Где возможно, переиспользуем настоящие ванильные Gui-классы (GuiChest,
 * GuiFurnace, GuiDispenser, GuiBrewingStand) поверх RemoteInventory —
 * это даёт точную ванильную текстуру/раскладку бесплатно. Для типов, которых
 * в 1.12.2 просто нет (grindstone/stonecutter/loom/smithing_table/lectern —
 * добавлены в 1.14+), которые устроены сложнее чем "просто IInventory"
 * (crafting_table — включает боевую крафт-логику; anvil — текстовое поле
 * названия; enchanting_table — завязан на опыт/книжные полки), или для
 * которых нужный ванильный класс не резолвится в текущем маппинге (hopper —
 * GuiHopper недоступен под snapshot_20171003, не стали гадать с альтернативным
 * именем), используется универсальный fallback GuiRemoteContainer.
 */
public final class RemoteContainerManager {

    private static final RemoteContainerManager INSTANCE = new RemoteContainerManager();
    public static RemoteContainerManager get() { return INSTANCE; }

    private static final Pattern CHEST_LIKE = Pattern.compile(".*(chest|shulker|barrel).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern FURNACE_LIKE = Pattern.compile(".*(furnace|smoker).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISPENSER_LIKE = Pattern.compile(".*(dispenser|dropper).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern BREWING = Pattern.compile(".*brewing.*", Pattern.CASE_INSENSITIVE);

    private RemoteInventory currentInventory;
    private String currentTitle;
    private Class<? extends GuiContainer> currentGuiClass;

    /** Вызывать при каждом "window" сообщении, чьё title != "Инвентарь". */
    public void refreshOrOpen(String title, List<RemoteWindowManager.SlotItem> slots) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;

        Class<? extends GuiContainer> targetClass = pickGuiClass(title);
        GuiScreen current = Minecraft.getMinecraft().currentScreen;

        boolean sameWindowAlreadyOpen = currentInventory != null
                && title.equals(currentTitle)
                && targetClass.equals(currentGuiClass)
                && targetClass.isInstance(current);

        if (sameWindowAlreadyOpen) {
            currentInventory.setContentsFromServer(title, slots);
            return;
        }

        currentInventory = new RemoteInventory();
        currentInventory.setContentsFromServer(title, slots);
        currentTitle = title;
        currentGuiClass = targetClass;

        GuiContainer gui = build(targetClass, player, currentInventory, title);
        Minecraft.getMinecraft().displayGuiScreen(gui);
    }

    private Class<? extends GuiContainer> pickGuiClass(String title) {
        if (CHEST_LIKE.matcher(title).matches()) return RemoteGuiChest.class;
        if (FURNACE_LIKE.matcher(title).matches()) return RemoteGuiFurnace.class;
        if (DISPENSER_LIKE.matcher(title).matches()) return RemoteGuiDispenser.class;
        if (BREWING.matcher(title).matches()) return RemoteGuiBrewingStand.class;
        return GuiRemoteContainer.class;
    }

    private GuiContainer build(Class<? extends GuiContainer> targetClass, EntityPlayerSP player,
                                RemoteInventory inv, String title) {
        try {
            if (targetClass == RemoteGuiChest.class) return new RemoteGuiChest(player.inventory, inv);
            if (targetClass == RemoteGuiFurnace.class) return new RemoteGuiFurnace(player.inventory, inv);
            if (targetClass == RemoteGuiDispenser.class) return new RemoteGuiDispenser(player.inventory, inv);
            if (targetClass == RemoteGuiBrewingStand.class) return new RemoteGuiBrewingStand(player.inventory, inv);
        } catch (Exception e) {
            com.mirror.clientmirror.ClientMirrorMod.LOGGER.warn(
                    "[clientmirror] Не удалось открыть ванильный GUI для '" + title + "', fallback: " + e);
        }
        RemoteContainer container = new RemoteContainer(inv, player.inventory);
        return new GuiRemoteContainer(container, player.inventory, title);
    }

    /** Вызывать на "windowClose" от сервера (сервер сам закрыл — игрок отошёл и т.п.). */
    public void closeIfOpen() {
        GuiScreen current = Minecraft.getMinecraft().currentScreen;
        if (currentGuiClass != null && currentGuiClass.isInstance(current)) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
        currentInventory = null;
        currentTitle = null;
        currentGuiClass = null;
    }
}
