package com.twixrpg.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Запускает таймер исчезновения Фарм-Меча.
 * <p>
 * В отличие от кирки и топора (у них отсчёт идёт с первого сломанного блока),
 * у меча 36 часов начинают тикать с ПЕРВОГО УДАРА по любому существу.
 */
public final class SwordHitListener implements Listener {

    private final TwixRPGItemsPlugin plugin;

    public SwordHitListener(TwixRPGItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile) {
            return; // урон стрелой/снарядом не считается ударом мечом
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!CustomItems.ID_SWORD.equals(CustomItems.getId(weapon))) {
            return;
        }
        if (CustomItems.hasFirstUse(weapon)) {
            return;
        }

        CustomItems.stampFirstUse(weapon);
        long hours = plugin.getExpireMs() / 3_600_000L;
        player.sendMessage(Component.text("⏳ ", NamedTextColor.GOLD)
                .append(Component.text("Уникальный предмет «" + CustomItems.friendlyName(weapon)
                        + "» активирован! Он исчезнет через " + hours + " ч.", NamedTextColor.YELLOW)));
    }
}
