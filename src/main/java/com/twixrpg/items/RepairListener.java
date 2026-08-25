package com.twixrpg.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Запрещает починку/совмещение/зачарование для «нечинящихся» предметов
 * (Аметистовая Кирка и Лабрис Гефеста). Фарм-Меч чинится свободно.
 */
public final class RepairListener implements Listener {

    private final Map<UUID, Long> lastNotify = new HashMap<>();

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);
        boolean firstBlocked = CustomItems.isNonRepairable(first);
        boolean secondBlocked = CustomItems.isNonRepairable(second);
        if ((firstBlocked && !isEmpty(second)) || (secondBlocked && !isEmpty(first))) {
            event.setResult(null);
            notify(event.getView().getPlayer(), "🔒 Этот предмет нельзя чинить или совмещать в наковальне!");
        }
    }

    @EventHandler
    public void onGrindstone(PrepareGrindstoneEvent event) {
        ItemStack upper = event.getInventory().getItem(0);
        ItemStack lower = event.getInventory().getItem(1);
        if (CustomItems.isNonRepairable(upper) || CustomItems.isNonRepairable(lower)) {
            event.setResult(null);
            notify(event.getView().getPlayer(), "🔒 Этот предмет нельзя чинить или разбирать в точиле!");
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (CustomItems.isNonRepairable(event.getItem())) {
            event.setCancelled(true);
            notify(event.getEnchanter(), "🔒 Этот предмет нельзя зачаровывать!");
        }
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    /** Сообщение с анти-спамом (не чаще раза в 1.5 секунды). */
    private void notify(HumanEntity human, String message) {
        long now = System.currentTimeMillis();
        long last = lastNotify.getOrDefault(human.getUniqueId(), 0L);
        if (now - last < 1500L) {
            return;
        }
        lastNotify.put(human.getUniqueId(), now);
        if (human instanceof Player player) {
            player.sendMessage(Component.text(message, NamedTextColor.RED));
        }
    }
}
