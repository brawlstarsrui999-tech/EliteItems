package com.twixrpg.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Обработчик ячеек хранения (рюкзаков).
 * <p>
 * При ПКМ по ячейке открывается GUI с фиолетовым оформлением.
 * В каждый слот можно положить до 1000 одинаковых предметов.
 * Данные хранятся в PDC предмета-ячейки.
 */
public final class StorageCellListener implements Listener {

    private final TwixRPGItemsPlugin plugin;
    private final Map<UUID, CellSession> sessions = new HashMap<>();

    public StorageCellListener(TwixRPGItemsPlugin plugin) {
        this.plugin = plugin;
    }

    // ==================== ОТКРЫТИЕ GUI (ПКМ) ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack hand = event.getItem();
        if (hand == null || !CustomItems.isStorageCell(hand)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();

        // Не открываем повторно
        if (sessions.containsKey(player.getUniqueId())) {
            return;
        }

        openCellGui(player, hand, event.getHand());
    }

    private void openCellGui(Player player, ItemStack cell, org.bukkit.inventory.EquipmentSlot hand) {
        String id = CustomItems.getId(cell);
        int slotCount = CustomItems.getCellSlotCount(id);
        if (slotCount <= 0) return;

        // Загружаем хранимые предметы из PDC
        ItemStack[] stored = loadSlots(cell, slotCount);

        // Создаём GUI
        int guiSize = getGuiSize(slotCount);
        int[] guiSlots = getGuiSlots(slotCount);

        StorageCellHolder holder = new StorageCellHolder(guiSize, getTitle(id));
        Inventory gui = holder.getInventory();

        // Заполняем фиолетовым стеклом
        ItemStack glass = createGlass();
        for (int i = 0; i < guiSize; i++) {
            gui.setItem(i, glass);
        }

        // Размещаем хранимые предметы (визуальные копии)
        for (int i = 0; i < slotCount; i++) {
            if (stored[i] != null) {
                gui.setItem(guiSlots[i], createVisualItem(stored[i], CustomItems.CELL_MAX_PER_SLOT));
            }
        }

        // Сохраняем сессию
        CellSession session = new CellSession(id, slotCount, guiSlots, stored);
        session.hand = hand;
        sessions.put(player.getUniqueId(), session);

        player.openInventory(gui);
    }

    // ==================== КЛИКИ В GUI ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageCellHolder)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        CellSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        Inventory topInventory = event.getInventory();

        // Клик в верхнем инвентаре (GUI ячейки)
        if (event.getClickedInventory() == topInventory) {
            event.setCancelled(true);

            int slot = event.getSlot();
            int storageIndex = session.guiSlotToStorageIndex(slot);
            if (storageIndex < 0) return; // Клик по стеклу

            ItemStack cursor = event.getCursor();
            ItemStack stored = session.stored[storageIndex];

            if (cursor == null || cursor.getType() == Material.AIR) {
                // === Забираем из слота ===
                if (stored != null) {
                    int takeAmount;
                    if (event.getClick().isRightClick()) {
                        takeAmount = 1;
                    } else {
                        takeAmount = Math.min(stored.getAmount(), stored.getMaxStackSize());
                    }
                    ItemStack taken = stored.clone();
                    taken.setAmount(takeAmount);

                    event.setCursor(taken);

                    int remaining = stored.getAmount() - takeAmount;
                    if (remaining <= 0) {
                        session.stored[storageIndex] = null;
                        topInventory.setItem(slot, createGlass()); // Заменяем на стекло
                    } else {
                        stored.setAmount(remaining);
                        topInventory.setItem(slot, createVisualItem(stored, CustomItems.CELL_MAX_PER_SLOT));
                    }
                }
            } else {
                // === Кладём в слот ===
                // Нельзя класть ячейки хранения внутрь ячеек
                if (CustomItems.isStorageCell(cursor)) {
                    player.sendMessage(Component.text("❌ Нельзя хранить ячейки внутри ячеек!",
                            TextColor.fromHexString("#ef4444")));
                    return;
                }

                if (stored == null) {
                    // Слот пустой — кладём
                    int canAdd = Math.min(cursor.getAmount(), CustomItems.CELL_MAX_PER_SLOT);
                    ItemStack newStored = cursor.clone();
                    newStored.setAmount(canAdd);
                    session.stored[storageIndex] = newStored;

                    int remaining = cursor.getAmount() - canAdd;
                    if (remaining <= 0) {
                        event.setCursor(null);
                    } else {
                        cursor.setAmount(remaining);
                    }
                    topInventory.setItem(slot, createVisualItem(newStored, CustomItems.CELL_MAX_PER_SLOT));

                } else if (stored.isSimilar(cursor)) {
                    // Тот же предмет — докладываем
                    int canAdd = Math.min(cursor.getAmount(),
                            CustomItems.CELL_MAX_PER_SLOT - stored.getAmount());
                    if (canAdd > 0) {
                        stored.setAmount(stored.getAmount() + canAdd);
                        int remaining = cursor.getAmount() - canAdd;
                        if (remaining <= 0) {
                            event.setCursor(null);
                        } else {
                            cursor.setAmount(remaining);
                        }
                        topInventory.setItem(slot, createVisualItem(stored, CustomItems.CELL_MAX_PER_SLOT));
                    } else {
                        player.sendMessage(Component.text("⚠ Слот заполнен! (макс. "
                                + CustomItems.CELL_MAX_PER_SLOT + ")", TextColor.fromHexString("#fbbf24")));
                    }
                } else {
                    // Другой предмет
                    player.sendMessage(Component.text("⚠ В этом слоте хранится другой предмет!",
                            TextColor.fromHexString("#fbbf24")));
                }
            }
            return;
        }

        // === Клик в нижнем инвентаре (инвентарь игрока) ===

        // Shift+клик из инвентаря игрока — пытаемся положить в ячейку
        if (event.getClick().isShiftClick()) {
            event.setCancelled(true);
            ItemStack current = event.getCurrentItem();
            if (current == null || current.getType() == Material.AIR) return;

            // Нельзя класть ячейки внутрь ячеек
            if (CustomItems.isStorageCell(current)) return;

            // Ищем подходящий слот (сначала с тем же предметом, потом пустой)
            for (int pass = 0; pass < 2; pass++) {
                for (int i = 0; i < session.slotCount && current.getAmount() > 0; i++) {
                    ItemStack stored = session.stored[i];
                    if (pass == 0 && stored != null && stored.isSimilar(current)) {
                        // Докладываем в слот с тем же предметом
                        int canAdd = Math.min(current.getAmount(),
                                CustomItems.CELL_MAX_PER_SLOT - stored.getAmount());
                        if (canAdd > 0) {
                            stored.setAmount(stored.getAmount() + canAdd);
                            current.setAmount(current.getAmount() - canAdd);
                            int guiSlot = session.storageToGui(i);
                            topInventory.setItem(guiSlot,
                                    createVisualItem(stored, CustomItems.CELL_MAX_PER_SLOT));
                        }
                    } else if (pass == 1 && stored == null) {
                        // Кладём в пустой слот
                        int canAdd = Math.min(current.getAmount(), CustomItems.CELL_MAX_PER_SLOT);
                        ItemStack newStored = current.clone();
                        newStored.setAmount(canAdd);
                        session.stored[i] = newStored;
                        current.setAmount(current.getAmount() - canAdd);
                        if (current.getAmount() <= 0) {
                            event.getClickedInventory().setItem(event.getSlot(), null);
                        }
                        int guiSlot = session.storageToGui(i);
                        topInventory.setItem(guiSlot,
                                createVisualItem(newStored, CustomItems.CELL_MAX_PER_SLOT));
                    }
                }
                if (current.getAmount() <= 0) break;
            }

            if (current.getAmount() > 0 && current.getType() != Material.AIR) {
                player.sendMessage(Component.text("⚠ Нет свободных слотов для этого предмета!",
                        TextColor.fromHexString("#fbbf24")));
            }
            return;
        }

        // Обычный клик в инвентаре игрока — запрещаем перемещение ячейки
        ItemStack current = event.getCurrentItem();
        if (current != null && CustomItems.isStorageCell(current)) {
            event.setCancelled(true);
        }

        // Запрещаем hotbar swap с ячейкой
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarSlot = event.getHotbarButton();
            ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
            if (hotbarItem != null && CustomItems.isStorageCell(hotbarItem)) {
                event.setCancelled(true);
            }
        }
    }

    // ==================== DRAG (отменяем) ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageCellHolder)) {
            return;
        }
        // Отменяем перетаскивание в GUI ячейки
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // ==================== ЗАКРЫТИЕ GUI ====================

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageCellHolder)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        CellSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        // Сохраняем содержимое в PDC ячейки
        saveToCell(player, session);
    }

    private void saveToCell(Player player, CellSession session) {
        // Ищем ячейку в инвентаре
        ItemStack cellItem = findCellInInventory(player, session);
        if (cellItem == null) {
            // Ячейка потеряна — выбрасываем содержимое
            for (ItemStack stored : session.stored) {
                if (stored != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), stored.clone());
                }
            }
            player.sendMessage(Component.text("⚠ Ячейка хранения потеряна! Предметы выброшены.",
                    TextColor.fromHexString("#ef4444")));
            return;
        }

        // Сохраняем слоты в PDC
        cellItem.editMeta(meta -> {
            for (int i = 0; i < session.slotCount; i++) {
                NamespacedKey key = new NamespacedKey(plugin, "cell_slot_" + i);
                if (session.stored[i] != null) {
                    meta.getPersistentDataContainer().set(key,
                            PersistentDataType.BYTE_ARRAY,
                            session.stored[i].serializeAsBytes());
                } else {
                    meta.getPersistentDataContainer().remove(key);
                }
            }
            // Обновляем lore с информацией о заполненности
            updateCellLore(meta, session);
        });
    }

    private ItemStack findCellInInventory(Player player, CellSession session) {
        // Сначала проверяем ожидаемую руку/слот
        if (session.hand == org.bukkit.inventory.EquipmentSlot.HAND) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && session.cellId.equals(CustomItems.getId(item))) {
                return item;
            }
        } else if (session.hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            ItemStack item = player.getInventory().getItemInOffHand();
            if (item != null && session.cellId.equals(CustomItems.getId(item))) {
                return item;
            }
        }

        // Ищем по всему инвентарю
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && session.cellId.equals(CustomItems.getId(item))) {
                return item;
            }
        }

        return null;
    }

    private void updateCellLore(ItemMeta meta, CellSession session) {
        List<Component> lore = new ArrayList<>();
        String id = session.cellId;

        TextColor accentColor = getAccentColor(id);
        String tierLabel = getTierLabel(id);

        lore.add(Component.text("✦  TWIXRPG • " + tierLabel + "  ✦", accentColor)
                .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        lore.add(Component.empty());

        int filledSlots = 0;
        for (ItemStack stored : session.stored) {
            if (stored != null) filledSlots++;
        }

        lore.add(Component.text("📦 Ячеек: ", TextColor.fromHexString("#c4b5fd"))
                .append(Component.text(session.slotCount, TextColor.fromHexString("#e9d5ff")))
                .append(Component.text(" (заполнено: ", TextColor.fromHexString("#8b8b8b")))
                .append(Component.text(filledSlots, TextColor.fromHexString("#e9d5ff")))
                .append(Component.text(")", TextColor.fromHexString("#8b8b8b")))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        lore.add(Component.text("💎 Макс. в слоте: ", TextColor.fromHexString("#c4b5fd"))
                .append(Component.text(CustomItems.CELL_MAX_PER_SLOT, TextColor.fromHexString("#e9d5ff")))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        lore.add(Component.empty());
        lore.add(Component.text("🖱 ПКМ — открыть хранилище", TextColor.fromHexString("#a78bfa"))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        lore.add(Component.empty());
        lore.add(Component.text("⚡ Бесконечная прочность", TextColor.fromHexString("#7dd3fc"))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        lore.add(Component.text("⚠ Предметы привязаны к ячейке", TextColor.fromHexString("#fca5a5"))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        meta.lore(lore);
    }

    // ==================== ЗАПРЕТ ИСПОЛЬЗОВАНИЯ КАК TRIM ====================

    @EventHandler
    public void onSmithing(PrepareSmithingEvent event) {
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && CustomItems.isStorageCell(item)) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onAnvilForCell(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);
        if ((first != null && CustomItems.isStorageCell(first))
                || (second != null && CustomItems.isStorageCell(second))) {
            event.setResult(null);
        }
    }

    // ==================== ЗАГРУЗКА / СОХРАНЕНИЕ ИЗ PDC ====================

    private ItemStack[] loadSlots(ItemStack cell, int slotCount) {
        ItemStack[] slots = new ItemStack[slotCount];
        if (!cell.hasItemMeta()) return slots;

        var pdc = cell.getItemMeta().getPersistentDataContainer();
        for (int i = 0; i < slotCount; i++) {
            NamespacedKey key = new NamespacedKey(plugin, "cell_slot_" + i);
            byte[] data = pdc.get(key, PersistentDataType.BYTE_ARRAY);
            if (data != null) {
                try {
                    slots[i] = ItemStack.deserializeBytes(data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Не удалось загрузить слот " + i + " ячейки: " + e.getMessage());
                }
            }
        }
        return slots;
    }

    // ==================== GUI: РАСКЛАДКА ====================

    /** Размер GUI инвентаря. */
    private static int getGuiSize(int slotCount) {
        if (slotCount <= 3) return 9;
        if (slotCount <= 6) return 18;
        if (slotCount <= 12) return 27;
        return 54; // 24 слота
    }

    /** Позиции слотов хранения в GUI. */
    private static int[] getGuiSlots(int slotCount) {
        if (slotCount == 3) {
            return new int[]{3, 4, 5};
        }
        if (slotCount == 6) {
            return new int[]{3, 4, 5, 12, 13, 14};
        }
        if (slotCount == 12) {
            return new int[]{
                    2, 3, 4, 5, 6, 7,
                    11, 12, 13, 14, 15, 16
            };
        }
        // 24 слота
        return new int[]{
                11, 12, 13, 14, 15, 16,
                20, 21, 22, 23, 24, 25,
                29, 30, 31, 32, 33, 34,
                38, 39, 40, 41, 42, 43
        };
    }

    // ==================== ВИЗУАЛЬНЫЕ ЭЛЕМЕНТЫ ====================

    /** Фиолетовое стекло для заполнения GUI. */
    private static ItemStack createGlass() {
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        glass.editMeta(meta -> meta.displayName(Component.text("✦",
                TextColor.fromHexString("#4c1d95"))));
        return glass;
    }

    /** Создаёт визуальную копию хранимого предмета для отображения в GUI. */
    private static ItemStack createVisualItem(ItemStack stored, int maxPerSlot) {
        ItemStack visual = stored.clone();
        // Показываем не более maxStackSize для красивого отображения
        int displayAmount = Math.min(stored.getAmount(), stored.getMaxStackSize());
        visual.setAmount(Math.max(1, displayAmount));

        ItemMeta meta = visual.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        lore.add(Component.empty());
        lore.add(Component.text("┃ ", TextColor.fromHexString("#7c3aed"))
                .append(Component.text("Хранится: ", TextColor.fromHexString("#a78bfa")))
                .append(Component.text(stored.getAmount(), TextColor.fromHexString("#e9d5ff"))
                        .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                .append(Component.text(" / ", TextColor.fromHexString("#6b7280")))
                .append(Component.text(maxPerSlot, TextColor.fromHexString("#c084fc")))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        meta.lore(lore);
        visual.setItemMeta(meta);
        return visual;
    }

    // ==================== ЗАГОЛОВКИ GUI ====================

    private static Component getTitle(String id) {
        return switch (id) {
            case CustomItems.ID_CELL_BASIC -> gradientTitle("✦ Ячейка Хранения ✦",
                    "#e9d5ff", "#a78bfa", "#6d28d9");
            case CustomItems.ID_CELL_ADVANCED -> gradientTitle("✦ Улучшенная Ячейка ✦",
                    "#d8b4fe", "#a855f7", "#581c87");
            case CustomItems.ID_CELL_HYBRID -> gradientTitle("✦ Гибридная Ячейка ✦",
                    "#f5d0fe", "#d946ef", "#86198f");
            case CustomItems.ID_CELL_PERFECT -> gradientTitle("✦ Совершенная Ячейка ✦",
                    "#fde047", "#c084fc", "#7c3aed", "#4c1d95");
            default -> Component.text("Ячейка Хранения", TextColor.fromHexString("#a78bfa"));
        };
    }

    private static Component gradientTitle(String text, String... hexColors) {
        TextColor[] colors = new TextColor[hexColors.length];
        for (int i = 0; i < hexColors.length; i++) {
            colors[i] = TextColor.fromHexString(hexColors[i]);
        }
        return gradient(text, colors)
                .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static Component gradient(String text, TextColor... colors) {
        var builder = Component.text();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            double t = length <= 1 ? 0.0 : (double) i / (length - 1);
            builder.append(Component.text(String.valueOf(text.charAt(i)), colorAt(colors, t)));
        }
        return builder.build();
    }

    private static TextColor colorAt(TextColor[] colors, double t) {
        if (colors.length <= 1) return colors.length == 0 ? TextColor.color(0xFFFFFF) : colors[0];
        double segment = t * (colors.length - 1);
        int index = (int) Math.floor(segment);
        if (index >= colors.length - 1) return colors[colors.length - 1];
        double local = segment - index;
        return TextColor.lerp((float) local, colors[index], colors[index + 1]);
    }

    // ==================== ВСПОМОГАТЕЛЬНОЕ ====================

    private static TextColor getAccentColor(String id) {
        return switch (id) {
            case CustomItems.ID_CELL_BASIC -> TextColor.fromHexString("#a78bfa");
            case CustomItems.ID_CELL_ADVANCED -> TextColor.fromHexString("#a855f7");
            case CustomItems.ID_CELL_HYBRID -> TextColor.fromHexString("#d946ef");
            case CustomItems.ID_CELL_PERFECT -> TextColor.fromHexString("#c084fc");
            default -> TextColor.fromHexString("#a78bfa");
        };
    }

    private static String getTierLabel(String id) {
        return switch (id) {
            case CustomItems.ID_CELL_BASIC -> "РЕДКИЙ ПРЕДМЕТ";
            case CustomItems.ID_CELL_ADVANCED -> "ЭПИЧЕСКИЙ ПРЕДМЕТ";
            case CustomItems.ID_CELL_HYBRID -> "ЛЕГЕНДАРНЫЙ ПРЕДМЕТ";
            case CustomItems.ID_CELL_PERFECT -> "МИФИЧЕСКИЙ ПРЕДМЕТ";
            default -> "УНИКАЛЬНЫЙ ПРЕДМЕТ";
        };
    }

    // ==================== ВНУТРЕННИЕ КЛАССЫ ====================

    /** Holder для GUI ячейки хранения. */
    public static class StorageCellHolder implements InventoryHolder {
        private final Inventory inventory;

        public StorageCellHolder(int size, Component title) {
            this.inventory = Bukkit.createInventory(this, size, title);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** Данные сессии открытой ячейки. */
    private static class CellSession {
        final String cellId;
        final int slotCount;
        final int[] guiSlots;
        final ItemStack[] stored;
        org.bukkit.inventory.EquipmentSlot hand;

        CellSession(String cellId, int slotCount, int[] guiSlots, ItemStack[] stored) {
            this.cellId = cellId;
            this.slotCount = slotCount;
            this.guiSlots = guiSlots;
            this.stored = stored;
        }

        /** Возвращает индекс слота хранения по GUI-слоту, или -1 если это стекло. */
        int guiSlotToStorageIndex(int guiSlot) {
            for (int i = 0; i < guiSlots.length; i++) {
                if (guiSlots[i] == guiSlot) return i;
            }
            return -1;
        }

        /** Возвращает GUI-слот по индексу слота хранения. */
        int storageToGui(int storageIndex) {
            return guiSlots[storageIndex];
        }
    }
}