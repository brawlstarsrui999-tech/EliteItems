package com.twixrpg.items;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TwixRPG Items — уникальные предметы для сервера TwixRPG (Minecraft 1.21.11).
 *
 * 1. Аметистовая Кирка (незеритовая кирка): Эффективность V, Удача III, копает 3x3,
 *    прочность +700..1000, исчезает через 36 часов после первого сломанного блока, чинить нельзя.
 * 2. Лабрис Гефеста (незеритовый топор): Эффективность V, рубит дерево целиком,
 *    прочность +700..1000, исчезает через 36 часов, чинить нельзя.
 * 3. Фарм-Меч (незеритовый меч): Добыча V, прочность +700..1000,
 *    исчезает через 36 часов, можно чинить в наковальне.
 */
public final class TwixRPGItemsPlugin extends JavaPlugin {

    private NamespacedKey itemIdKey;
    private NamespacedKey firstUseKey;

    private long expireMs;
    private int pickaxeMaxDurability;
    private int axeMaxDurability;
    private int swordMaxDurability;
    private int treeMaxLogs;
    private int treeMaxLeaves;
    private boolean sneakDisablesAoe;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        itemIdKey = new NamespacedKey(this, "item_id");
        firstUseKey = new NamespacedKey(this, "first_use");

        CustomItems.init(this);

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new RepairListener(), this);

        TwixCommand command = new TwixCommand(this);
        PluginCommand pluginCommand = getCommand("twixitems");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        // Проверка истечения срока действия каждые 30 секунд (старт через 5 секунд)
        getServer().getScheduler().runTaskTimer(this, new ExpiryManager(this), 100L, 600L);

        getLogger().info("TwixRPGItems включён! Уникальные предметы загружены.");
    }

    @Override
    public void onDisable() {
        getLogger().info("TwixRPGItems выключен.");
    }

    private void loadConfig() {
        long hours = Math.max(1, getConfig().getInt("expire-hours", 36));
        expireMs = hours * 3_600_000L;
        pickaxeMaxDurability = getConfig().getInt("items.pickaxe-max-durability", 2900);
        axeMaxDurability = getConfig().getInt("items.axe-max-durability", 2900);
        swordMaxDurability = getConfig().getInt("items.sword-max-durability", 2900);
        treeMaxLogs = getConfig().getInt("tree-feller.max-logs", 512);
        treeMaxLeaves = getConfig().getInt("tree-feller.max-leaves", 1024);
        sneakDisablesAoe = getConfig().getBoolean("aoe.sneak-disables", false);
    }

    public NamespacedKey getItemIdKey() {
        return itemIdKey;
    }

    public NamespacedKey getFirstUseKey() {
        return firstUseKey;
    }

    public long getExpireMs() {
        return expireMs;
    }

    public int getPickaxeMaxDurability() {
        return pickaxeMaxDurability;
    }

    public int getAxeMaxDurability() {
        return axeMaxDurability;
    }

    public int getSwordMaxDurability() {
        return swordMaxDurability;
    }

    public int getTreeMaxLogs() {
        return treeMaxLogs;
    }

    public int getTreeMaxLeaves() {
        return treeMaxLeaves;
    }

    public boolean isSneakDisablesAoe() {
        return sneakDisablesAoe;
    }
}
