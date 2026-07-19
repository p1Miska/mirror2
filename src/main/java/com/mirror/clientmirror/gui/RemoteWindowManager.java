package com.mirror.clientmirror.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ЭТАП 4 (заготовка).
 *
 * Здесь пока только хранение последних данных hotbar/window, пришедших с сервера,
 * плюс точки расширения. Полноценная реализация — это:
 *
 *  1) RemoteSlotItem — POD с name/displayName/count (+ прицепить ResourceLocation
 *     иконки через маппинг предметов, аналогично BlockMapper, для тех предметов
 *     которых в 1.12.2 нет).
 *  2) RemoteInventoryContainer extends Container — НЕ содержит реальных ItemStack
 *     с точки зрения ванильной логики крафта/стакания (сервер — источник истины),
 *     а рисует ровно то, что лежит в `slots`. Это "read-through" контейнер:
 *     слоты помечаются как non-placeable, а клик игрока не двигает предмет локально —
 *     вместо этого клик сразу уходит в WS (windowClick) и локальное отображение
 *     обновится только когда придёт следующий "window"/"hotbar" от сервера
 *     (тот же принцип "authoritative server", что и в п.4 требований про блоки).
 *  3) RemoteInventoryGui extends GuiContainer — рисует фон в зависимости от
 *     msg.title (совпадает по regex с CONTAINER_BLOCK_RE на сервере: chest -> 27/54
 *     слотов, furnace -> отдельный layout с топливом/прогрессом, crafting_table -> 3x3 и т.д.)
 *     Тут нужен реальный пример payload "window" с сервера, чтобы точно сматчить
 *     количество/раскладку слотов на конкретный GUI-текстур из vanilla assets —
 *     тяну этот шаг до подтверждения формата.
 *
 * Пока: hotbar уже можно повесить на HUD-оверлей (RenderGameOverlayEvent) поверх
 * ванильного, т.к. локальный bot.inventory игрока всё равно пуст (мир без сервера).
 */
public class RemoteWindowManager {

    private static final RemoteWindowManager INSTANCE = new RemoteWindowManager();
    public static RemoteWindowManager get() { return INSTANCE; }

    public static final class SlotItem {
        public final String name;
        public final String displayName;
        public final int count;
        public SlotItem(String name, String displayName, int count) {
            this.name = name;
            this.displayName = displayName;
            this.count = count;
        }
    }

    private volatile List<SlotItem> hotbar = new ArrayList<>();
    private volatile int selectedSlot = 0;
    private volatile String openWindowTitle = null;
    private volatile List<SlotItem> openWindowSlots = new ArrayList<>();

    public void applyHotbar(JsonObject msg) {
        selectedSlot = msg.has("selected") ? msg.get("selected").getAsInt() : 0;
        hotbar = parseSlots(msg.getAsJsonArray("hotbar"));
        // armor: {head,chest,legs,feet} — хранение аналогично, опущено для краткости MVP
    }

    public void applyWindow(JsonObject msg) {
        openWindowTitle = msg.has("title") ? msg.get("title").getAsString() : "";
        openWindowSlots = parseSlots(msg.getAsJsonArray("slots"));
    }

    public void closeWindow() {
        openWindowTitle = null;
        openWindowSlots = new ArrayList<>();
    }

    private List<SlotItem> parseSlots(JsonArray arr) {
        List<SlotItem> result = new ArrayList<>();
        if (arr == null) return result;
        for (int i = 0; i < arr.size(); i++) {
            if (arr.get(i).isJsonNull()) {
                result.add(null);
                continue;
            }
            JsonObject item = arr.get(i).getAsJsonObject();
            result.add(new SlotItem(
                    item.get("name").getAsString(),
                    item.has("displayName") ? item.get("displayName").getAsString() : item.get("name").getAsString(),
                    item.has("count") ? item.get("count").getAsInt() : 1
            ));
        }
        return result;
    }

    public List<SlotItem> getHotbar() { return hotbar; }
    public int getSelectedSlot() { return selectedSlot; }
    public String getOpenWindowTitle() { return openWindowTitle; }
    public List<SlotItem> getOpenWindowSlots() { return openWindowSlots; }
}
