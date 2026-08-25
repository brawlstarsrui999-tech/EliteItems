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

/**
 * Фабрика уникальных предметов TwixRPG.
 */
public final class CustomItems {

    public static final String ID_PICKAXE = "amethyst_pickaxe";
    public static final String ID_AXE = "labrys";
    public static final String ID_SWORD = "farm_sword";

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
