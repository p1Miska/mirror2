package com.mirror.clientmirror.item;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Приблизительный маппинг предметов на 1.12.2, тем же принципом что и BlockMapper:
 * точное совпадение -> ручные алиасы -> эвристики по суффиксу -> fallback.
 *
 * Специально отмечено в задаче: незерит (появился в 1.16) сводим к алмазному
 * аналогу (netherite_chestplate -> diamond_chestplate и т.д.) — по механике это
 * ближе всего. Эльтра (elytra) в 1.12.2 УЖЕ существует (добавлена в 1.9), маппинг
 * не нужен, идёт по прямому совпадению.
 */
public final class ItemMapper {

    private static final Map<String, String> EXACT_ALIASES = new HashMap<>();
    static {
        // Незерит -> алмаз (ближайший по редкости/механике инструмент/броня)
        for (String piece : new String[]{"helmet", "chestplate", "leggings", "boots",
                "sword", "pickaxe", "axe", "shovel", "hoe"}) {
            EXACT_ALIASES.put("netherite_" + piece, "diamond_" + piece);
        }
        EXACT_ALIASES.put("netherite_ingot", "diamond");
        EXACT_ALIASES.put("netherite_block", "diamond_block");
        EXACT_ALIASES.put("netherite_scrap", "diamond");
        EXACT_ALIASES.put("copper_ingot", "iron_ingot");
        EXACT_ALIASES.put("raw_iron", "iron_ingot");
        EXACT_ALIASES.put("raw_gold", "gold_ingot");
        EXACT_ALIASES.put("raw_copper", "iron_ingot");
        EXACT_ALIASES.put("spyglass", "stick");
        EXACT_ALIASES.put("goat_horn", "stick");
        EXACT_ALIASES.put("recovery_compass", "compass");
        EXACT_ALIASES.put("echo_shard", "diamond");
        EXACT_ALIASES.put("disc_fragment_5", "stick");
        EXACT_ALIASES.put("mace", "iron_sword");
        EXACT_ALIASES.put("wind_charge", "snowball");
        EXACT_ALIASES.put("bundle", "chest");
    }

    private static final String[] NEW_WOOD_TYPES = {
            "cherry", "mangrove", "bamboo", "crimson", "warped", "pale_oak", "azalea"
    };

    private static final Pattern PLANKS = Pattern.compile(".*_planks$");
    private static final Pattern LOG = Pattern.compile(".*_log$|.*_stem$|.*_hyphae$");
    private static final Pattern BOAT = Pattern.compile(".*_boat$");
    private static final Pattern DOOR = Pattern.compile(".*_door$");
    private static final Pattern SIGN = Pattern.compile(".*_sign$|.*_hanging_sign$");
    private static final Pattern SPAWN_EGG = Pattern.compile(".*_spawn_egg$");
    private static final Pattern SMITHING_TEMPLATE = Pattern.compile(".*_smithing_template$");
    private static final Pattern POTTERY_SHERD = Pattern.compile(".*_pottery_sherd$");

    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    public static ItemStack resolve(String rawName, int count) {
        String name = stripNamespace(rawName);
        ItemStack template = CACHE.get(name);
        if (template == null) {
            Item item = doResolve(name);
            template = new ItemStack(item);
            CACHE.put(name, template);
        }
        ItemStack result = template.copy();
        result.setCount(Math.max(1, count));
        return result;
    }

    private static Item doResolve(String name) {
        Item direct = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("minecraft", name));
        if (direct != null && direct != Items.AIR) return direct;

        if (EXACT_ALIASES.containsKey(name)) {
            Item aliased = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("minecraft", EXACT_ALIASES.get(name)));
            if (aliased != null) return aliased;
        }

        String normalized = name;
        for (String wood : NEW_WOOD_TYPES) {
            if (normalized.startsWith(wood + "_")) {
                normalized = normalized.replaceFirst("^" + wood + "_", "oak_");
                break;
            }
        }
        if (!normalized.equals(name)) {
            Item byWood = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("minecraft", normalized));
            if (byWood != null && byWood != Items.AIR) return byWood;
        }

        if (PLANKS.matcher(name).matches()) return Item.getItemFromBlock(net.minecraft.init.Blocks.PLANKS);
        if (LOG.matcher(name).matches()) return Item.getItemFromBlock(net.minecraft.init.Blocks.LOG);
        if (BOAT.matcher(name).matches()) return Items.BOAT;
        if (DOOR.matcher(name).matches()) return Items.OAK_DOOR;
        if (SIGN.matcher(name).matches()) return Items.SIGN;
        if (SPAWN_EGG.matcher(name).matches()) return Items.SPAWN_EGG;
        if (SMITHING_TEMPLATE.matcher(name).matches()) return Items.PAPER;
        if (POTTERY_SHERD.matcher(name).matches()) return Items.CLAY_BALL;

        // Полный fallback — булыжник как максимально нейтральный "предмет-заглушка"
        return Item.getItemFromBlock(net.minecraft.init.Blocks.COBBLESTONE);
    }

    private static String stripNamespace(String raw) {
        int idx = raw.indexOf(':');
        return idx >= 0 ? raw.substring(idx + 1) : raw;
    }

    private ItemMapper() {}
}
