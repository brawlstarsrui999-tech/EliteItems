package com.twixrpg.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Периодически проверяет инвентари игроков и удаляет предметы,
 * у которых истёк срок действия (36 часов с первого сломанного блока).
 */
public final class ExpiryManager implements Runnable {

    private final TwixRPGItemsPlugin plugin;

    public ExpiryManager(TwixRPGItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            scan(player, player.getInventory(), now);
            scan(player, player.getEnderChest(), now);
        }
    }

    private void scan(Player player, Inventory inventory, long now) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null) {
                continue;
            }
            if (CustomItems.isCustom(item) && CustomItems.isExpired(item, now)) {
                String name = CustomItems.friendlyName(item);
                inventory.setItem(slot, null);
                player.sendMessage(Component.text(
                        "⏳ Ваш уникальный предмет «" + name + "» истёк и исчез.",
                        NamedTextColor.RED));
            }
        }
    }
}