package com.twixrpg.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик ячеек хранения (рюкзаков).
 * <p>
 * Содержимое хранится в PDC самого предмета-ячейки (не как эндерсундук):
 * любой, у кого в руках эта ячейка, видит те же вещи.
 */
public final class StorageCellListener implements Listener {

    private final TwixRPGItemsPlugin plugin;
    private final Map<UUID, CellSession> sessions = new HashMap<>();

    public StorageCellListener(TwixRPGItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void saveAllOpen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof StorageCellHolder) {
                player.closeInventory();
            }
        }
        sessions.clear();
    }

    // ==================== ОТКРЫТИЕ GUI (ПКМ) ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack cell = player.getInventory().getItem(hand);
        if (cell == null || !CustomItems.isStorageCell(cell)) {
            return;
        }

        event.setCancelled(true);

        if (sessions.containsKey(player.getUniqueId())) {
            return;
        }

        openCellGui(player, cell, hand);
    }

    private void openCellGui(Player player, ItemStack cell, EquipmentSlot hand) {
        String id = CustomItems.getId(cell);
        int slotCount = CustomItems.getCellSlotCount(id);
        if (slotCount <= 0) {
            return;
        }

        cell = unstackIfNeeded(player, cell, hand, slotCount);
        String uuid = ensureCellUuid(player, cell, hand);
        cell = player.getInventory().getItem(hand);
        if (cell == null || !CustomItems.isStorageCell(cell)) {
            return;
        }

        int maxPerSlot = CustomItems.getCellMaxPerSlot(id);
        StoredItem[] stored = loadSlots(cell, slotCount);

        int guiSize = getGuiSize(slotCount);
        int[] guiSlots = getGuiSlots(slotCount);

        CellSession session = new CellSession(id, uuid, slotCount, maxPerSlot, guiSlots, stored, hand);
        StorageCellHolder holder = new StorageCellHolder(guiSize, getTitle(id), session);
        Inventory gui = holder.getInventory();
        paintGui(gui, session);

        sessions.put(player.getUniqueId(), session);
        player.openInventory(gui);
    }

    /**
     * Шаблоны брони стакаются до 64. Ячейки должны быть по одной,
     * иначе содержимое двух предметов смешается.
     */
    private ItemStack unstackIfNeeded(Player player, ItemStack cell, EquipmentSlot hand, int slotCount) {
        if (cell.getAmount() <= 1) {
            return cell;
        }
        ItemStack extra = cell.clone();
        extra.setAmount(cell.getAmount() - 1);
        extra.editMeta(meta -> {
            meta.getPersistentDataContainer().set(
                    plugin.getCellUuidKey(), PersistentDataType.STRING, UUID.randomUUID().toString());
            clearStorageKeys(meta, slotCount);
        });
        extra.setData(DataComponentTypes.MAX_STACK_SIZE, 1);

        ItemStack one = cell.clone();
        one.setAmount(1);
        one.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        player.getInventory().setItem(hand, one);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(extra);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        return one;
    }

    private String ensureCellUuid(Player player, ItemStack cell, EquipmentSlot hand) {
        String uuid = CustomItems.getCellUuid(cell);
        if (uuid != null && !uuid.isEmpty()) {
            return uuid;
        }
        uuid = UUID.randomUUID().toString();
        ItemStack updated = cell.clone();
        String stamp = uuid;
        updated.editMeta(meta -> meta.getPersistentDataContainer().set(
                plugin.getCellUuidKey(), PersistentDataType.STRING, stamp));
        updated.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        player.getInventory().setItem(hand, updated);
        return uuid;
    }

    // ==================== КЛИКИ В GUI ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageCellHolder holder)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        CellSession session = holder.session;
        Inventory topInventory = event.getInventory();

        if (event.getClick() == ClickType.DOUBLE_CLICK
                || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getClick() == ClickType.UNKNOWN) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarSlot = event.getHotbarButton();
            ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
            if (hotbarItem != null && CustomItems.isStorageCell(hotbarItem)) {
                event.setCancelled(true);
                return;
            }
        }

        boolean clickedTop = event.getRawSlot() >= 0 && event.getRawSlot() < topInventory.getSize();

        if (clickedTop) {
            event.setCancelled(true);

            int storageIndex = session.guiSlotToStorageIndex(event.getSlot());
            if (storageIndex < 0) {
                return;
            }

            ItemStack cursor = cloneOrNull(event.getCursor());
            StoredItem stored = session.stored[storageIndex];

            if (isEmpty(cursor)) {
                if (stored == null || stored.amount <= 0) {
                    return;
                }
                int takeAmount = event.getClick().isRightClick()
                        ? 1
                        : Math.min(stored.amount, stored.template.getMaxStackSize());
                takeAmount = Math.min(takeAmount, stored.amount);
                if (takeAmount <= 0) {
                    return;
                }
                ItemStack taken = stored.take(takeAmount);
                if (stored.amount <= 0) {
                    session.stored[storageIndex] = null;
                }
                applyClickResult(player, session, topInventory, taken);
                return;
            }

            if (CustomItems.isStorageCell(cursor)) {
                player.sendMessage(Component.text("❌ Нельзя хранить ячейки внутри ячеек!",
                        TextColor.fromHexString("#ef4444")));
                return;
            }

            int requested = event.getClick().isRightClick() ? 1 : cursor.getAmount();
            if (stored == null) {
                int canAdd = Math.min(requested, session.maxPerSlot);
                session.stored[storageIndex] = StoredItem.from(cursor, canAdd);
                cursor.setAmount(cursor.getAmount() - canAdd);
                applyClickResult(player, session, topInventory, cursor.getAmount() > 0 ? cursor : null);
                return;
            }

            if (!stored.matches(cursor)) {
                player.sendMessage(Component.text("⚠ В этом слоте хранится другой предмет!",
                        TextColor.fromHexString("#fbbf24")));
                return;
            }

            int canAdd = Math.min(requested, session.maxPerSlot - stored.amount);
            if (canAdd <= 0) {
                player.sendMessage(Component.text("⚠ Слот заполнен! (макс. "
                        + session.maxPerSlot + ")", TextColor.fromHexString("#fbbf24")));
                return;
            }
            stored.amount += canAdd;
            cursor.setAmount(cursor.getAmount() - canAdd);
            applyClickResult(player, session, topInventory, cursor.getAmount() > 0 ? cursor : null);
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current != null && CustomItems.isStorageCell(current)) {
            event.setCancelled(true);
            return;
        }

        if (!event.getClick().isShiftClick()) {
            return;
        }

        event.setCancelled(true);
        if (isEmpty(current)) {
            return;
        }
        if (CustomItems.isStorageCell(current)) {
            return;
        }

        ItemStack moving = current.clone();
        depositFromInventory(session, moving);

        int leftover = moving.getAmount();
        ItemStack result = leftover > 0 ? moving : null;
        if (leftover > 0 && leftover == current.getAmount()) {
            player.sendMessage(Component.text("⚠ Нет свободных слотов для этого предмета!",
                    TextColor.fromHexString("#fbbf24")));
        }

        Inventory clicked = event.getClickedInventory();
        int clickedSlot = event.getSlot();
        if (clicked != null) {
            clicked.setItem(clickedSlot, result);
        }
        paintGui(topInventory, session);
        persist(player, session, false);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (clicked != null) {
                clicked.setItem(clickedSlot, result);
            }
            paintGui(topInventory, session);
            persist(player, session, false);
        });
    }

    private void depositFromInventory(CellSession session, ItemStack moving) {
        for (int pass = 0; pass < 2 && moving.getAmount() > 0; pass++) {
            for (int i = 0; i < session.slotCount && moving.getAmount() > 0; i++) {
                StoredItem stored = session.stored[i];
                if (pass == 0) {
                    if (stored == null || !stored.matches(moving)) {
                        continue;
                    }
                    int canAdd = Math.min(moving.getAmount(), session.maxPerSlot - stored.amount);
                    if (canAdd > 0) {
                        stored.amount += canAdd;
                        moving.setAmount(moving.getAmount() - canAdd);
                    }
                } else if (stored == null) {
                    int canAdd = Math.min(moving.getAmount(), session.maxPerSlot);
                    session.stored[i] = StoredItem.from(moving, canAdd);
                    moving.setAmount(moving.getAmount() - canAdd);
                }
            }
        }
    }

    private void applyClickResult(Player player, CellSession session, Inventory gui, ItemStack newCursor) {
        ItemStack cursorCopy = cloneOrNull(newCursor);
        paintGui(gui, session);
        persist(player, session, false);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.setItemOnCursor(cursorCopy);
            paintGui(gui, session);
            persist(player, session, false);
        });
    }

    // ==================== DRAG (отменяем в GUI) ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageCellHolder)) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!sessions.containsKey(player.getUniqueId())) {
            return;
        }
        if (CustomItems.isStorageCell(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CellSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            persist(player, session, true);
        }
    }

    // ==================== ЗАКРЫТИЕ GUI ====================

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageCellHolder holder)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        sessions.remove(player.getUniqueId());
        persist(player, holder.session, true);
    }

    private boolean persist(Player player, CellSession session, boolean dropIfMissing) {
        int slot = findCellSlot(player, session);
        if (slot < 0) {
            if (!dropIfMissing) {
                return false;
            }
            for (StoredItem stored : session.stored) {
                if (stored == null || stored.amount <= 0) {
                    continue;
                }
                dropStored(player, stored);
            }
            player.sendMessage(Component.text("⚠ Ячейка хранения потеряна! Предметы выброшены.",
                    TextColor.fromHexString("#ef4444")));
            return false;
        }

        PlayerInventory inv = player.getInventory();
        ItemStack updated = inv.getItem(slot);
        if (updated == null || updated.getType().isAir()) {
            return false;
        }
        updated = updated.clone();
        writeContents(updated, session);
        updated.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        inv.setItem(slot, updated);
        return true;
    }

    private int findCellSlot(Player player, CellSession session) {
        PlayerInventory inv = player.getInventory();
        if (session.hand != null) {
            ItemStack inHand = inv.getItem(session.hand);
            if (isSessionCell(inHand, session)) {
                return slotOfHand(player, session.hand);
            }
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (isSessionCell(inv.getItem(i), session)) {
                return i;
            }
        }
        return -1;
    }

    private static int slotOfHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            return 40;
        }
        return player.getInventory().getHeldItemSlot();
    }

    private boolean isSessionCell(ItemStack item, CellSession session) {
        if (item == null || !CustomItems.isStorageCell(item)) {
            return false;
        }
        String uuid = CustomItems.getCellUuid(item);
        if (session.cellUuid != null && session.cellUuid.equals(uuid)) {
            return true;
        }
        return uuid == null && session.cellId.equals(CustomItems.getId(item));
    }

    private void writeContents(ItemStack cell, CellSession session) {
        cell.editMeta(meta -> {
            var pdc = meta.getPersistentDataContainer();
            if (session.cellUuid != null) {
                pdc.set(plugin.getCellUuidKey(), PersistentDataType.STRING, session.cellUuid);
            }
            for (int i = 0; i < session.slotCount; i++) {
                pdc.remove(oldSlotKey(i));
                StoredItem stored = session.stored[i];
                if (stored == null || stored.amount <= 0) {
                    pdc.remove(itemKey(i));
                    pdc.remove(amtKey(i));
                    session.stored[i] = null;
                    continue;
                }
                try {
                    ItemStack template = stored.template.clone();
                    template.setAmount(1);
                    String encoded = Base64.getEncoder().encodeToString(template.serializeAsBytes());
                    pdc.set(itemKey(i), PersistentDataType.STRING, encoded);
                    pdc.set(amtKey(i), PersistentDataType.INTEGER, stored.amount);
                } catch (Exception e) {
                    plugin.getLogger().warning("Не удалось сохранить слот " + i + " ячейки: " + e.getMessage());
                }
            }
            updateCellLore(meta, session);
        });
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
        for (StoredItem stored : session.stored) {
            if (stored != null && stored.amount > 0) {
                filledSlots++;
            }
        }

        lore.add(Component.text("📦 Ячеек: ", TextColor.fromHexString("#c4b5fd"))
                .append(Component.text(session.slotCount, TextColor.fromHexString("#e9d5ff")))
                .append(Component.text(" (заполнено: ", TextColor.fromHexString("#8b8b8b")))
                .append(Component.text(filledSlots, TextColor.fromHexString("#e9d5ff")))
                .append(Component.text(")", TextColor.fromHexString("#8b8b8b")))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        lore.add(Component.text("💎 Макс. в слоте: ", TextColor.fromHexString("#c4b5fd"))
                .append(Component.text(session.maxPerSlot, TextColor.fromHexString("#e9d5ff")))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        lore.add(Component.empty());
        lore.add(Component.text("🖱 ПКМ — открыть хранилище", TextColor.fromHexString("#a78bfa"))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        lore.add(Component.empty());
        lore.add(Component.text("⚡ Бесконечная прочность", TextColor.fromHexString("#7dd3fc"))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        lore.add(Component.text("⚠ Предметы хранятся в этой ячейке", TextColor.fromHexString("#fca5a5"))
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

    private StoredItem[] loadSlots(ItemStack cell, int slotCount) {
        StoredItem[] slots = new StoredItem[slotCount];
        if (!cell.hasItemMeta()) {
            return slots;
        }

        var pdc = cell.getItemMeta().getPersistentDataContainer();
        for (int i = 0; i < slotCount; i++) {
            String encoded = pdc.get(itemKey(i), PersistentDataType.STRING);
            Integer amount = pdc.get(amtKey(i), PersistentDataType.INTEGER);
            if (encoded != null && !encoded.isEmpty() && amount != null && amount > 0) {
                try {
                    ItemStack template = ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
                    if (template != null && !template.getType().isAir()) {
                        template.setAmount(1);
                        slots[i] = new StoredItem(template, amount);
                        continue;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Не удалось загрузить слот " + i + " ячейки: " + e.getMessage());
                }
            }

            byte[] legacy = pdc.get(oldSlotKey(i), PersistentDataType.BYTE_ARRAY);
            if (legacy != null) {
                try {
                    ItemStack stack = ItemStack.deserializeBytes(legacy);
                    if (stack != null && !stack.getType().isAir()) {
                        int amt = Math.max(1, stack.getAmount());
                        stack.setAmount(1);
                        slots[i] = new StoredItem(stack, amt);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Не удалось загрузить старый слот " + i + " ячейки: " + e.getMessage());
                }
            }
        }
        return slots;
    }

    private void clearStorageKeys(ItemMeta meta, int slotCount) {
        var pdc = meta.getPersistentDataContainer();
        for (int i = 0; i < slotCount; i++) {
            pdc.remove(itemKey(i));
            pdc.remove(amtKey(i));
            pdc.remove(oldSlotKey(i));
        }
    }

    private NamespacedKey itemKey(int index) {
        return new NamespacedKey(plugin, "cell_i_" + index);
    }

    private NamespacedKey amtKey(int index) {
        return new NamespacedKey(plugin, "cell_a_" + index);
    }

    private NamespacedKey oldSlotKey(int index) {
        return new NamespacedKey(plugin, "cell_slot_" + index);
    }

    // ==================== GUI: РАСКЛАДКА ====================

    private static int getGuiSize(int slotCount) {
        if (slotCount <= 6) {
            return 27;
        }
        if (slotCount <= 12) {
            return 36;
        }
        return 54;
    }

    private static int[] getGuiSlots(int slotCount) {
        if (slotCount == 3) {
            return new int[]{12, 13, 14};
        }
        if (slotCount == 6) {
            return new int[]{11, 12, 13, 14, 15, 16};
        }
        if (slotCount == 12) {
            return new int[]{
                    10, 11, 12, 13, 14, 15,
                    19, 20, 21, 22, 23, 24
            };
        }
        return new int[]{
                11, 12, 13, 14, 15, 16,
                20, 21, 22, 23, 24, 25,
                29, 30, 31, 32, 33, 34,
                38, 39, 40, 41, 42, 43
        };
    }

    private static void paintGui(Inventory gui, CellSession session) {
        ItemStack border = createBorderGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, border);
        }
        for (int i = 0; i < session.slotCount; i++) {
            StoredItem stored = session.stored[i];
            if (stored != null && stored.amount > 0) {
                gui.setItem(session.guiSlots[i], createVisualItem(stored, session.maxPerSlot));
            } else {
                gui.setItem(session.guiSlots[i], createSlotGlass());
            }
        }
    }

    // ==================== ВИЗУАЛЬНЫЕ ЭЛЕМЕНТЫ ====================

    /** Фиолетовое стекло — рамка, класть предметы нельзя. */
    private static ItemStack createBorderGlass() {
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        glass.editMeta(meta -> meta.displayName(Component.text(" ")
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        return glass;
    }

    /** Магентовое стекло — пустой слот хранения, сюда можно класть предметы. */
    private static ItemStack createSlotGlass() {
        ItemStack glass = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        glass.editMeta(meta -> {
            meta.displayName(Component.text("Пустой слот", TextColor.fromHexString("#f0abfc"))
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.lore(List.of(
                    Component.text("Сюда можно положить предметы", TextColor.fromHexString("#c4b5fd"))
                            .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            ));
        });
        return glass;
    }

    private static ItemStack createVisualItem(StoredItem stored, int maxPerSlot) {
        ItemStack visual = stored.template.clone();
        int displayAmount = Math.min(Math.max(1, stored.amount), visual.getMaxStackSize());
        visual.setAmount(displayAmount);

        ItemMeta meta = visual.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null
                ? new ArrayList<>(meta.lore())
                : new ArrayList<>();

        lore.add(Component.empty());
        lore.add(Component.text("┃ ", TextColor.fromHexString("#7c3aed"))
                .append(Component.text("Хранится: ", TextColor.fromHexString("#a78bfa")))
                .append(Component.text(stored.amount, TextColor.fromHexString("#e9d5ff"))
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
        if (colors.length <= 1) {
            return colors.length == 0 ? TextColor.color(0xFFFFFF) : colors[0];
        }
        double segment = t * (colors.length - 1);
        int index = (int) Math.floor(segment);
        if (index >= colors.length - 1) {
            return colors[colors.length - 1];
        }
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

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        if (isEmpty(item)) {
            return null;
        }
        return item.clone();
    }

    private static void dropStored(Player player, StoredItem stored) {
        int left = stored.amount;
        int max = Math.max(1, stored.template.getMaxStackSize());
        while (left > 0) {
            int n = Math.min(left, max);
            ItemStack drop = stored.template.clone();
            drop.setAmount(n);
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
            left -= n;
        }
    }

    // ==================== ВНУТРЕННИЕ КЛАССЫ ====================

    public static final class StorageCellHolder implements InventoryHolder {
        private final Inventory inventory;
        private final CellSession session;

        public StorageCellHolder(int size, Component title, CellSession session) {
            this.session = session;
            this.inventory = Bukkit.createInventory(this, size, title);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class CellSession {
        final String cellId;
        final String cellUuid;
        final int slotCount;
        final int maxPerSlot;
        final int[] guiSlots;
        final StoredItem[] stored;
        final EquipmentSlot hand;

        CellSession(String cellId, String cellUuid, int slotCount, int maxPerSlot,
                    int[] guiSlots, StoredItem[] stored, EquipmentSlot hand) {
            this.cellId = cellId;
            this.cellUuid = cellUuid;
            this.slotCount = slotCount;
            this.maxPerSlot = maxPerSlot;
            this.guiSlots = guiSlots;
            this.stored = stored;
            this.hand = hand;
        }

        int guiSlotToStorageIndex(int guiSlot) {
            for (int i = 0; i < guiSlots.length; i++) {
                if (guiSlots[i] == guiSlot) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Шаблон предмета (amount=1) + реальное количество.
     * Количество хранится отдельно, чтобы не упираться в лимит стака 99.
     */
    private static final class StoredItem {
        final ItemStack template;
        int amount;

        StoredItem(ItemStack template, int amount) {
            ItemStack copy = template.clone();
            copy.setAmount(1);
            this.template = copy;
            this.amount = amount;
        }

        static StoredItem from(ItemStack source, int amount) {
            return new StoredItem(source, amount);
        }

        boolean matches(ItemStack other) {
            if (other == null || other.getType().isAir()) {
                return false;
            }
            return template.isSimilar(other);
        }

        ItemStack take(int count) {
            ItemStack out = template.clone();
            out.setAmount(count);
            amount -= count;
            return out;
        }
    }
}