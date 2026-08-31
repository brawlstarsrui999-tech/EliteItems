package com.twixrpg.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Отвечает за особые свойства предметов: добыча 3x3 (кирка),
 * рубка дерева целиком (топор), а также запуск таймера исчезновения.
 */
public final class BlockBreakListener implements Listener {

    private final TwixRPGItemsPlugin plugin;

    public BlockBreakListener(TwixRPGItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        String id = CustomItems.getId(tool);
        if (id == null) {
            return;
        }

        // Первый сломанный блок — запускаем таймер (36 часов).
        // Только для кирки и топора: у меча отсчёт идёт с первого удара,
        // а ячейки хранения вечные и таймера не имеют вовсе.
        boolean timerTool = CustomItems.ID_PICKAXE.equals(id) || CustomItems.ID_AXE.equals(id);
        if (timerTool && !CustomItems.hasFirstUse(tool)) {
            CustomItems.stampFirstUse(tool);
            long hours = plugin.getExpireMs() / 3_600_000L;
            player.sendMessage(Component.text("⏳ ", NamedTextColor.GOLD)
                    .append(Component.text("Уникальный предмет «" + CustomItems.friendlyName(tool)
                            + "» активирован! Он исчезнет через " + hours + " ч.", NamedTextColor.YELLOW)));
        }

        boolean sneaking = plugin.isSneakDisablesAoe() && player.isSneaking();

        if (CustomItems.ID_PICKAXE.equals(id) && !sneaking) {
            mine3x3(event, player);
        } else if (CustomItems.ID_AXE.equals(id) && !sneaking
                && Tag.LOGS.isTagged(event.getBlock().getType())) {
            fellTree(event, player);
        }
    }

    // ==================== КОПАЕТ 3×3 ====================

    private void mine3x3(BlockBreakEvent event, Player player) {
        Block center = event.getBlock();
        BlockFace face = faceFromPlayer(player);

        List<Block> area = new ArrayList<>(9);
        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    area.add(center.getRelative(dx, 0, dz));
                }
            }
        } else {
            BlockFace horizontal = (face == BlockFace.NORTH || face == BlockFace.SOUTH)
                    ? BlockFace.EAST : BlockFace.NORTH;
            for (int d = -1; d <= 1; d++) {
                for (int dy = -1; dy <= 1; dy++) {
                    area.add(center.getRelative(horizontal, d).getRelative(BlockFace.UP, dy));
                }
            }
        }

        for (Block block : area) {
            if (block.equals(center)) {
                continue;
            }
            if (!isBreakable(block)) {
                continue;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (CustomItems.getId(hand) == null) {
                return; // инструмент сломался во время работы
            }
            breakExtra(block, player, hand);
        }
    }

    // ==================== РУБКА ДЕРЕВА ЦЕЛИКОМ ====================

    private void fellTree(BlockBreakEvent event, Player player) {
        Block start = event.getBlock();

        // Все брёвна, соединённые со сломанным (BFS)
        Set<Block> logs = new LinkedHashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        logs.add(start);
        queue.add(start);
        int maxLogs = plugin.getTreeMaxLogs();
        while (!queue.isEmpty() && logs.size() < maxLogs) {
            Block current = queue.poll();
            for (BlockFace face : BlockFace.values()) {
                Block next = current.getRelative(face);
                if (Tag.LOGS.isTagged(next.getType()) && logs.add(next)) {
                    queue.add(next);
                }
            }
        }

        // Листва, прилегающая к брёвнам и соединённая между собой
        Set<Block> leaves = new LinkedHashSet<>();
        Deque<Block> leafQueue = new ArrayDeque<>();
        for (Block log : logs) {
            for (BlockFace face : BlockFace.values()) {
                Block next = log.getRelative(face);
                if (Tag.LEAVES.isTagged(next.getType()) && leaves.add(next)) {
                    leafQueue.add(next);
                }
            }
        }
        int maxLeaves = plugin.getTreeMaxLeaves();
        while (!leafQueue.isEmpty() && leaves.size() < maxLeaves) {
            Block current = leafQueue.poll();
            for (BlockFace face : BlockFace.values()) {
                Block next = current.getRelative(face);
                if (Tag.LEAVES.isTagged(next.getType()) && leaves.add(next)) {
                    leafQueue.add(next);
                }
            }
        }

        for (Block block : logs) {
            if (block.equals(start)) {
                continue;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (CustomItems.getId(hand) == null) {
                break;
            }
            breakExtra(block, player, hand);
        }
        for (Block block : leaves) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (CustomItems.getId(hand) == null) {
                break;
            }
            breakExtra(block, player, hand);
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНОЕ ====================

    /** Ломает блок с дропом, как если бы это делал игрок этим инструментом (с учётом Удачи и т.п.). */
    private void breakExtra(Block block, Player player, ItemStack tool) {
        if (!isBreakable(block)) {
            return;
        }
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        Collection<ItemStack> drops = block.getDrops(tool);
        block.setType(Material.AIR);
        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(dropLocation, drop);
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            tool.damage(1, player);
        }
    }

    private static boolean isBreakable(Block block) {
        Material type = block.getType();
        if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
            return false;
        }
        if (type == Material.BEDROCK || type == Material.BARRIER) {
            return false;
        }
        if (type == Material.WATER || type == Material.LAVA) {
            return false;
        }
        return type.getHardness() >= 0;
    }

    /** Определяет грань, на которую смотрит игрок, чтобы выбрать плоскость для 3×3. */
    private static BlockFace faceFromPlayer(Player player) {
        float pitch = player.getLocation().getPitch();
        if (pitch < -45f) {
            return BlockFace.UP;
        }
        if (pitch > 45f) {
            return BlockFace.DOWN;
        }
        float yaw = player.getLocation().getYaw();
        double rotation = ((yaw % 360) + 360) % 360;
        if (rotation >= 315 || rotation < 45) {
            return BlockFace.SOUTH;
        }
        if (rotation >= 45 && rotation < 135) {
            return BlockFace.WEST;
        }
        if (rotation >= 135 && rotation < 225) {
            return BlockFace.NORTH;
        }
        return BlockFace.EAST;
    }
}
