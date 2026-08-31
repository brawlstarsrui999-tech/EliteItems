package com.twixrpg.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ограничивает, куда можно класть ячейки хранения.
 * <p>
 * Разрешено: инвентарь игрока, обычные сундуки (в т.ч. двойные и сундуки-ловушки)
 * и бочки.
 * <p>
 * Запрещено: шалкеры, эндер-сундуки, банди (мешки), воронки, раздатчики,
 * другие ячейки и любые прочие контейнеры. Иначе можно было бы набить шалкер
 * полными ячейками и положить этот шалкер в ещё одну ячейку — вместимость
 * становится бесконечной.
 */
public final class CellContainerListener implements Listener {

    private static final TextColor DENY_COLOR = TextColor.fromHexString("#ef4444");
    private static final String DENY_MESSAGE =
            "❌ Ячейку хранения можно класть только в обычные сундуки и бочки!";

    private final Map<UUID, Long> lastNotify = new HashMap<>();

    // ==================== КЛИКИ В ИНВЕНТАРЕ ====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Запрет складывания ячейки в мешок (bundle) — он тоже носит предметы с собой
        if (isBundleSwap(event.getCursor(), event.getCurrentItem())) {
            event.setCancelled(true);
            notifyPlayer(player);
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof StorageCellListener.StorageCellHolder) {
            return; // GUI самой ячейки — обрабатывается в StorageCellListener
        }
        if (isAllowedContainer(top)) {
            return;
        }

        boolean clickedTop = event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize();

        if (clickedTop) {
            // Кладём курсором
            if (CustomItems.isStorageCell(event.getCursor())) {
                event.setCancelled(true);
                notifyPlayer(player);
                return;
            }
            // Кладём цифрой (1-9)
            if (event.getClick() == ClickType.NUMBER_KEY) {
                ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
                if (CustomItems.isStorageCell(hotbar)) {
                    event.setCancelled(true);
                    notifyPlayer(player);
                    return;
                }
            }
            // Кладём предметом из левой руки (F)
            if (event.getClick() == ClickType.SWAP_OFFHAND
                    && CustomItems.isStorageCell(player.getInventory().getItemInOffHand())) {
                event.setCancelled(true);
                notifyPlayer(player);
            }
            return;
        }

        // Shift-клик из инвентаря игрока — предмет уедет в верхний контейнер
        if (event.isShiftClick() && CustomItems.isStorageCell(event.getCurrentItem())) {
            event.setCancelled(true);
            notifyPlayer(player);
        }
    }

    // ==================== ПЕРЕТАСКИВАНИЕ ====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!CustomItems.isStorageCell(event.getOldCursor())) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof StorageCellListener.StorageCellHolder) {
            return;
        }
        if (isAllowedContainer(top)) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < top.getSize()) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    notifyPlayer(player);
                }
                return;
            }
        }
    }

    // ==================== ВОРОНКИ / РАЗДАТЧИКИ ====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMoveItem(InventoryMoveItemEvent event) {
        if (!CustomItems.isStorageCell(event.getItem())) {
            return;
        }
        if (!isAllowedContainer(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    // ==================== ПРОВЕРКИ ====================

    /** Разрешён ли этот инвентарь для хранения ячеек. */
    private static boolean isAllowedContainer(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        InventoryType type = inventory.getType();
        return switch (type) {
            // Собственный инвентарь игрока
            case PLAYER, CRAFTING -> true;
            // Бочка
            case BARREL -> true;
            // Обычный сундук / сундук-ловушка / двойной сундук.
            // Тип CHEST также у вагонеток, лодок с сундуком и GUI других плагинов —
            // их пропускаем, поэтому дополнительно проверяем владельца инвентаря.
            case CHEST -> isRealChest(inventory.getHolder());
            default -> false;
        };
    }

    private static boolean isRealChest(InventoryHolder holder) {
        if (holder instanceof DoubleChest doubleChest) {
            return doubleChest.getLeftSide() instanceof Chest
                    || doubleChest.getRightSide() instanceof Chest;
        }
        return holder instanceof Chest;
    }

    private static boolean isBundleSwap(ItemStack cursor, ItemStack clicked) {
        return (isBundle(cursor) && CustomItems.isStorageCell(clicked))
                || (CustomItems.isStorageCell(cursor) && isBundle(clicked));
    }

    private static boolean isBundle(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        // BUNDLE, WHITE_BUNDLE, RED_BUNDLE и т.д.
        return item.getType().name().endsWith("BUNDLE");
    }

    /** Сообщение с анти-спамом (не чаще раза в 1.5 секунды). */
    private void notifyPlayer(HumanEntity human) {
        long now = System.currentTimeMillis();
        long last = lastNotify.getOrDefault(human.getUniqueId(), 0L);
        if (now - last < 1500L) {
            return;
        }
        lastNotify.put(human.getUniqueId(), now);
        if (human instanceof Player player) {
            player.sendMessage(Component.text(DENY_MESSAGE, DENY_COLOR)
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
    }
}
