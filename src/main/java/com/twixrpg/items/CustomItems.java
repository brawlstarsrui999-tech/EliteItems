package com.twixrpg.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

/**
 * Фабрика уникальных предметов TwixRPG.
 */
public final class CustomItems {

    public static final String ID_PICKAXE = "amethyst_pickaxe";
    public static final String ID_AXE = "labrys";
    public static final String ID_SWORD = "farm_sword";

    // Ячейки хранения
    public static final String ID_CELL_BASIC = "storage_cell_basic";
    public static final String ID_CELL_ADVANCED = "storage_cell_advanced";
    public static final String ID_CELL_HYBRID = "storage_cell_hybrid";
    public static final String ID_CELL_PERFECT = "storage_cell_perfect";

    public static final int CELL_MAX_BASIC = 512;
    public static final int CELL_MAX_ADVANCED = 1024;
    public static final int CELL_MAX_HYBRID = 2048;
    public static final int CELL_MAX_PERFECT = 4096;

    private static TwixRPGItemsPlugin plugin;

    private CustomItems() {
    }

    public static void init(TwixRPGItemsPlugin p) {
        plugin = p;
    }

    // ==================== СОЗДАНИЕ ПРЕДМЕТОВ ====================

    /**
     * ⛏ Аметистовая Кирка — незеритовая кирка.
     * Эффективность V, Удача III, копает 3x3. Нельзя чинить.
     */
    public static ItemStack createPickaxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        TextColor c1 = hex("#e9d5ff");
        TextColor c2 = hex("#a78bfa");
        TextColor c3 = hex("#5b21b6");

        item.editMeta(meta -> {
            meta.displayName(gradient("Аметистовая Кирка", c1, c2, c3)
                    .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.lore(List.of(
                    accent("✦  ТWIXRPG • УНИКАЛЬНЫЙ ПРЕДМЕТ  ✦", "#a78bfa"),
                    Component.empty(),
                    stat("⚡ Эффективность V", "#86efac"),
                    stat("💎 Удача III", "#86efac"),
                    stat("⛏ Копает 3×3 (три на три)", "#86efac"),
                    stat("🛡 Повышенная прочность: " + plugin.getPickaxeMaxDurability(), "#7dd3fc"),
                    Component.empty(),
                    warn("⏳ Исчезнет через 36 часов после первого сломанного блока"),
                    warn("🔒 Нельзя чинить (наковальня, точило, зачарование)")
            ));
            meta.getPersistentDataContainer().set(plugin.getItemIdKey(), PersistentDataType.STRING, ID_PICKAXE);
        });

        item.setData(DataComponentTypes.MAX_DAMAGE, plugin.getPickaxeMaxDurability());
        item.setData(DataComponentTypes.DAMAGE, 0);
        item.setData(DataComponentTypes.ENCHANTMENTS, ItemEnchantments.itemEnchantments()
                .add(Enchantment.EFFICIENCY, 5)
                .add(Enchantment.FORTUNE, 3)
                .build());
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return item;
    }

    /**
     * 🪓 Лабрис Гефеста — незеритовый топор.
     * Эффективность V, рубит дерево целиком. Нельзя чинить.
     */
    public static ItemStack createAxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        TextColor c1 = hex("#ffe08a");
        TextColor c2 = hex("#ff8c42");
        TextColor c3 = hex("#dc2626");

        item.editMeta(meta -> {
            meta.displayName(gradient("Лабрис Гефеста", c1, c2, c3)
                    .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.lore(List.of(
                    accent("🔥  ТWIXRPG • УНИКАЛЬНЫЙ ПРЕДМЕТ  🔥", "#fb923c"),
                    Component.empty(),
                    stat("⚡ Эффективность V", "#86efac"),
                    stat("🪓 Рубит дерево целиком (все брёвна и листва)", "#86efac"),
                    stat("🛡 Повышенная прочность: " + plugin.getAxeMaxDurability(), "#7dd3fc"),
                    Component.empty(),
                    warn("⏳ Исчезнет через 36 часов после первого сломанного блока"),
                    warn("🔒 Нельзя чинить (наковальня, точило, зачарование)")
            ));
            meta.getPersistentDataContainer().set(plugin.getItemIdKey(), PersistentDataType.STRING, ID_AXE);
        });

        item.setData(DataComponentTypes.MAX_DAMAGE, plugin.getAxeMaxDurability());
        item.setData(DataComponentTypes.DAMAGE, 0);
        item.setData(DataComponentTypes.ENCHANTMENTS, ItemEnchantments.itemEnchantments()
                .add(Enchantment.EFFICIENCY, 5)
                .build());
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return item;
    }

    /**
     * ⚔ Фарм-Меч — незеритовый меч.
     * Добыча V. Можно чинить в наковальне.
     */
    public static ItemStack createSword() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        TextColor c1 = hex("#fde047");
        TextColor c2 = hex("#84cc16");
        TextColor c3 = hex("#15803d");

        item.editMeta(meta -> {
            meta.displayName(gradient("Фарм-Меч", c1, c2, c3)
                    .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.lore(List.of(
                    accent("⚔  ТWIXRPG • УНИКАЛЬНЫЙ ПРЕДМЕТ  ⚔", "#84cc16"),
                    Component.empty(),
                    stat("🗡 Добыча V", "#86efac"),
                    stat("🛡 Повышенная прочность: " + plugin.getSwordMaxDurability(), "#7dd3fc"),
                    Component.empty(),
                    warn("⏳ Исчезнет через 36 часов после первого сломанного блока"),
                    good("🔧 Можно чинить в наковальне")
            ));
            meta.getPersistentDataContainer().set(plugin.getItemIdKey(), PersistentDataType.STRING, ID_SWORD);
        });

        item.setData(DataComponentTypes.MAX_DAMAGE, plugin.getSwordMaxDurability());
        item.setData(DataComponentTypes.DAMAGE, 0);
        item.setData(DataComponentTypes.ENCHANTMENTS, ItemEnchantments.itemEnchantments()
                .add(Enchantment.LOOTING, 5)
                .build());
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return item;
    }

    // ==================== ЯЧЕЙКИ ХРАНЕНИЯ ====================

    /**
     * 🎒 Ячейка Хранения — 3 слота по 512 предметов.
     * Модель: sentry_armor_trim_smithing_template
     */
    public static ItemStack createStorageCellBasic() {
        return createStorageCell(
                Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
                ID_CELL_BASIC,
                "Ячейка Хранения",
                "РЕДКИЙ ПРЕДМЕТ",
                3,
                hex("#e9d5ff"), hex("#a78bfa"), hex("#6d28d9"),
                "#a78bfa"
        );
    }

    /**
     * 🎒 Улучшенная Ячейка Хранения — 6 слотов по 1024 предметов.
     * Модель: dune_armor_trim_smithing_template
     */
    public static ItemStack createStorageCellAdvanced() {
        return createStorageCell(
                Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
                ID_CELL_ADVANCED,
                "Улучшенная Ячейка Хранения",
                "ЭПИЧЕСКИЙ ПРЕДМЕТ",
                6,
                hex("#d8b4fe"), hex("#a855f7"), hex("#581c87"),
                "#a855f7"
        );
    }

    /**
     * 🎒 Гибридная Ячейка Хранения — 12 слотов по 2048 предметов.
     * Модель: spire_armor_trim_smithing_template
     */
    public static ItemStack createStorageCellHybrid() {
        return createStorageCell(
                Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
                ID_CELL_HYBRID,
                "Гибридная Ячейка Хранения",
                "ЛЕГЕНДАРНЫЙ ПРЕДМЕТ",
                12,
                hex("#f5d0fe"), hex("#d946ef"), hex("#86198f"),
                "#d946ef"
        );
    }

    /**
     * 🎒 Совершенная Ячейка Хранения — 24 слота по 4096 предметов.
     * Модель: rib_armor_trim_smithing_template
     */
    public static ItemStack createStorageCellPerfect() {
        return createStorageCell(
                Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
                ID_CELL_PERFECT,
                "Совершенная Ячейка Хранения",
                "МИФИЧЕСКИЙ ПРЕДМЕТ",
                24,
                hex("#fde047"), hex("#c084fc"), hex("#7c3aed"),
                "#c084fc"
        );
    }

    private static ItemStack createStorageCell(
            Material material,
            String id,
            String name,
            String tier,
            int slots,
            TextColor c1, TextColor c2, TextColor c3,
            String accentHex
    ) {
        ItemStack item = new ItemStack(material);

        item.editMeta(meta -> {
            meta.displayName(gradient(name, c1, c2, c3)
                    .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

            meta.lore(List.of(
                    accent("✦  TWIXRPG • " + tier + "  ✦", accentHex),
                    Component.empty(),
                    stat("📦 Ячеек: " + slots, "#c4b5fd"),
                    stat("💎 Макс. в слоте: " + getCellMaxPerSlot(id), "#c4b5fd"),
                    stat("🎒 Тип: Рюкзак", "#c4b5fd"),
                    Component.empty(),
                    stat("🖱 ПКМ — открыть хранилище", "#a78bfa"),
                    Component.empty(),
                    stat("⚡ Бесконечная прочность", "#7dd3fc"),
                    warn("⚠ Предметы хранятся в этой ячейке")
            ));

            meta.getPersistentDataContainer().set(plugin.getItemIdKey(), PersistentDataType.STRING, id);
            meta.getPersistentDataContainer().set(plugin.getCellUuidKey(), PersistentDataType.STRING, UUID.randomUUID().toString());
        });

        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return item;
    }

    // ==================== ИДЕНТИФИКАЦИЯ ====================

    public static String getId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getItemIdKey(), PersistentDataType.STRING);
    }

    public static boolean isCustom(ItemStack item) {
        return getId(item) != null;
    }

    /**
     * Предмет нельзя чинить/зачаровывать (кирка и топор). Меч — можно.
     */
    public static boolean isNonRepairable(ItemStack item) {
        String id = getId(item);
        return ID_PICKAXE.equals(id) || ID_AXE.equals(id);
    }

    /**
     * Проверяет, является ли предмет ячейкой хранения.
     */
    public static boolean isStorageCell(ItemStack item) {
        String id = getId(item);
        return id != null && getCellSlotCount(id) > 0;
    }

    /**
     * Возвращает количество слотов для ячейки хранения.
     */
    public static int getCellSlotCount(String id) {
        if (id == null) {
            return 0;
        }
        return switch (id) {
            case ID_CELL_BASIC -> 3;
            case ID_CELL_ADVANCED -> 6;
            case ID_CELL_HYBRID -> 12;
            case ID_CELL_PERFECT -> 24;
            default -> 0;
        };
    }

    /**
     * Максимум предметов в одном слоте ячейки (зависит от тира).
     */
    public static int getCellMaxPerSlot(String id) {
        if (id == null) {
            return CELL_MAX_BASIC;
        }
        return switch (id) {
            case ID_CELL_BASIC -> CELL_MAX_BASIC;
            case ID_CELL_ADVANCED -> CELL_MAX_ADVANCED;
            case ID_CELL_HYBRID -> CELL_MAX_HYBRID;
            case ID_CELL_PERFECT -> CELL_MAX_PERFECT;
            default -> CELL_MAX_BASIC;
        };
    }

    /**
     * Уникальный UUID конкретной ячейки (содержимое привязано к предмету, не к игроку).
     */
    public static String getCellUuid(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getCellUuidKey(), PersistentDataType.STRING);
    }

    public static boolean hasFirstUse(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getFirstUseKey(), PersistentDataType.LONG);
    }

    public static void stampFirstUse(ItemStack item) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(
                plugin.getFirstUseKey(), PersistentDataType.LONG, System.currentTimeMillis()));
    }

    public static boolean isExpired(ItemStack item, long now) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Long firstUse = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getFirstUseKey(), PersistentDataType.LONG);
        if (firstUse == null) {
            return false;
        }
        return (now - firstUse) >= plugin.getExpireMs();
    }

    public static String friendlyName(ItemStack item) {
        String id = getId(item);
        if (id == null) {
            return "Уникальный предмет";
        }
        return switch (id) {
            case ID_PICKAXE -> "Аметистовая Кирка";
            case ID_AXE -> "Лабрис Гефеста";
            case ID_SWORD -> "Фарм-Меч";
            case ID_CELL_BASIC -> "Ячейка Хранения";
            case ID_CELL_ADVANCED -> "Улучшенная Ячейка Хранения";
            case ID_CELL_HYBRID -> "Гибридная Ячейка Хранения";
            case ID_CELL_PERFECT -> "Совершенная Ячейка Хранения";
            default -> "Уникальный предмет";
        };
    }

    // ==================== ОФОРМЛЕНИЕ ====================

    /** Градиент по всем переданным цветам. */
    private static Component gradient(String text, TextColor... colors) {
        TextComponent.Builder builder = Component.text();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            double t = length <= 1 ? 0.0 : (double) i / (length - 1);
            builder.append(Component.text(String.valueOf(text.charAt(i)), colorAt(colors, t)));
        }
        return builder.build();
    }

    private static TextColor colorAt(TextColor[] colors, double t) {
        if (colors.length == 0) {
            return TextColor.color(0xFFFFFF);
        }
        if (colors.length == 1) {
            return colors[0];
        }
        double segment = t * (colors.length - 1);
        int index = (int) Math.floor(segment);
        if (index >= colors.length - 1) {
            return colors[colors.length - 1];
        }
        double local = segment - index;
        return TextColor.lerp((float) local, colors[index], colors[index + 1]);
    }

    private static TextColor hex(String value) {
        TextColor color = TextColor.fromHexString(value);
        return color != null ? color : TextColor.color(0xFFFFFF);
    }

    private static Component accent(String text, String hexColor) {
        return Component.text(text, hex(hexColor))
                .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static Component stat(String text, String hexColor) {
        return Component.text(text, hex(hexColor))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static Component warn(String text) {
        return Component.text(text, hex("#fca5a5"))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static Component good(String text) {
        return Component.text(text, hex("#86efac"))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
