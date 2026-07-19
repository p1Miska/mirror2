package com.mirror.clientmirror.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Приблизительный маппинг блоков современной версии (сервер шлёт имена вида
 * "minecraft:oak_log", "minecraft:copper_block", "minecraft:cherry_planks" и т.п.,
 * многих из которых в 1.12.2 просто не существует) на ближайший аналог 1.12.2.
 *
 * Это НЕ точная эмуляция — цель дать "похожую" картинку мира, не более.
 * Правила проверяются по порядку: точное совпадение -> явные ручные алиасы ->
 * эвристики по подстрокам (_planks, _log, _stairs, _slab, _wool, _terracotta...) ->
 * fallback.
 */
public final class BlockMapper {

    // Точные переименования, где 1.12.2 блок называется иначе, чем в новых версиях.
    private static final Map<String, String> EXACT_ALIASES = new HashMap<>();
    static {
        EXACT_ALIASES.put("grass_block", "grass");
        EXACT_ALIASES.put("dirt_path", "grass_path");
        EXACT_ALIASES.put("short_grass", "tallgrass"); // meta 1
        EXACT_ALIASES.put("cobblestone_wall", "cobblestone_wall");
        EXACT_ALIASES.put("torch", "torch");
        EXACT_ALIASES.put("redstone_torch", "redstone_torch");
        EXACT_ALIASES.put("sunflower", "double_plant"); // meta 0
        EXACT_ALIASES.put("cave_air", "air");
        EXACT_ALIASES.put("void_air", "air");
        EXACT_ALIASES.put("water", "water");
        EXACT_ALIASES.put("lava", "lava");
        EXACT_ALIASES.put("bedrock", "bedrock");
        EXACT_ALIASES.put("copper_block", "iron_block");
        EXACT_ALIASES.put("exposed_copper", "iron_block");
        EXACT_ALIASES.put("weathered_copper", "iron_block");
        EXACT_ALIASES.put("oxidized_copper", "iron_block");
        EXACT_ALIASES.put("deepslate", "stone");
        EXACT_ALIASES.put("tuff", "andesite");
        EXACT_ALIASES.put("calcite", "diorite");
        EXACT_ALIASES.put("mud", "dirt");
        EXACT_ALIASES.put("mangrove_roots", "oak_fence");
        EXACT_ALIASES.put("amethyst_block", "purple_stained_glass");
    }

    // Материалы деревьев, которых в 1.12.2 не существовало (появились позже) -> ближайшее дерево.
    private static final String[] NEW_WOOD_TYPES = {
            "cherry", "mangrove", "bamboo", "crimson", "warped", "pale_oak", "azalea"
    };

    private static final Pattern LOG = Pattern.compile(".*_log$|.*_stem$");
    private static final Pattern PLANKS = Pattern.compile(".*_planks$");
    private static final Pattern STAIRS = Pattern.compile(".*_stairs$");
    private static final Pattern SLAB = Pattern.compile(".*_slab$");
    private static final Pattern WOOL = Pattern.compile(".*_wool$");
    private static final Pattern TERRACOTTA = Pattern.compile(".*_terracotta$");
    private static final Pattern CONCRETE = Pattern.compile(".*_concrete$");
    private static final Pattern GLASS = Pattern.compile(".*_stained_glass$");
    private static final Pattern LEAVES = Pattern.compile(".*_leaves$");
    private static final Pattern DOOR = Pattern.compile(".*_door$");
    private static final Pattern FENCE = Pattern.compile(".*_fence$");
    private static final Pattern SIGN = Pattern.compile(".*_sign$|.*_hanging_sign$");
    private static final Pattern COPPER = Pattern.compile(".*copper.*");

    private static final Map<String, IBlockState> CACHE = new HashMap<>();

    /** blockName без namespace, например "oak_log" или "copper_block". */
    public static IBlockState resolve(String rawName) {
        String name = stripNamespace(rawName);
        if (CACHE.containsKey(name)) return CACHE.get(name);

        IBlockState state = doResolve(name);
        CACHE.put(name, state);
        return state;
    }

    private static IBlockState doResolve(String name) {
        // 1. Прямое совпадение по реестру 1.12.2
        Block direct = ForgeRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation("minecraft", name));
        if (direct != null && direct != Blocks.AIR) {
            return direct.getDefaultState();
        }

        // 2. Ручные алиасы
        if (EXACT_ALIASES.containsKey(name)) {
            Block aliased = ForgeRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation("minecraft", EXACT_ALIASES.get(name)));
            if (aliased != null) return aliased.getDefaultState();
        }

        // 3. Эвристики по суффиксу (_stairs/_slab/_door/_fence/...) — ПЕРВЫМИ,
        // потому что они сохраняют ФОРМУ блока (полублок/ступенька/дверь), а не
        // только текстуру. Для меди/новых металлов в 1.12.2 просто нет ни плиты,
        // ни лестницы (их не завезли даже намного позже) — поэтому проходимость
        // важнее цвета: "cut_copper_stairs" лучше показать деревянной лестницей
        // (форма верная, цвет нет), чем цельным железным кубом (перекроет проход).
        String normalized = name;
        for (String wood : NEW_WOOD_TYPES) {
            if (normalized.startsWith(wood + "_")) {
                normalized = normalized.replaceFirst("^" + wood + "_", "oak_");
                break;
            }
        }

        Block byHeuristic = resolveHeuristic(normalized);
        if (byHeuristic != null) return byHeuristic.getDefaultState();

        // 4. Медь/новые металлы БЕЗ формового суффикса (просто "copper_block",
        // "cut_copper", "copper_bulb" и т.п. — по факту это всегда цельный куб
        // и в оригинале) — тут цвет важнее, ставим железо.
        if (COPPER.matcher(name).matches()) {
            return Blocks.IRON_BLOCK.getDefaultState();
        }

        // 5. Fallback — видимый, но нейтральный блок-заглушка (не барьер: барьер invisible)
        return Blocks.STONE.getDefaultState();
    }

    private static Block resolveHeuristic(String name) {
        if (LOG.matcher(name).matches()) return Blocks.LOG;
        if (PLANKS.matcher(name).matches()) return Blocks.PLANKS;
        if (STAIRS.matcher(name).matches()) return Blocks.OAK_STAIRS;
        if (SLAB.matcher(name).matches()) return Blocks.WOODEN_SLAB;
        if (WOOL.matcher(name).matches()) return Blocks.WOOL;
        if (TERRACOTTA.matcher(name).matches()) return Blocks.STAINED_HARDENED_CLAY;
        if (CONCRETE.matcher(name).matches()) return Blocks.CONCRETE;
        if (GLASS.matcher(name).matches()) return Blocks.STAINED_GLASS;
        if (LEAVES.matcher(name).matches()) return Blocks.LEAVES;
        if (DOOR.matcher(name).matches()) return Blocks.OAK_DOOR;
        if (FENCE.matcher(name).matches()) return Blocks.OAK_FENCE;
        if (SIGN.matcher(name).matches()) return Blocks.STANDING_SIGN;
        return null;
    }

    private static String stripNamespace(String raw) {
        int idx = raw.indexOf(':');
        return idx >= 0 ? raw.substring(idx + 1) : raw;
    }

    private BlockMapper() {}
}
