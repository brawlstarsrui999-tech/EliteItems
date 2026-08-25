package com.twixrpg.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Команда /twixitems — выдача уникальных предметов.
 */
public final class TwixCommand implements CommandExecutor, TabCompleter {

    private final TwixRPGItemsPlugin plugin;

    public TwixCommand(TwixRPGItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("twixitems.give")) {
            sender.sendMessage(Component.text("У вас нет прав на выдачу предметов.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Использование: /twixitems give <pickaxe|axe|sword> [ник]", NamedTextColor.RED));
            return;
        }

        ItemStack item = switch (args[1].toLowerCase()) {
            case "pickaxe", "кирка" -> CustomItems.createPickaxe();
            case "axe", "топор" -> CustomItems.createAxe();
            case "sword", "меч" -> CustomItems.createSword();
            default -> null;
        };
        if (item == null) {
            sender.sendMessage(Component.text("Неизвестный предмет: " + args[1] + ". Доступно: pickaxe, axe, sword", NamedTextColor.RED));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            target = null;
        }

        if (target == null) {
            sender.sendMessage(Component.text("Игрок не найден или не в сети.", NamedTextColor.RED));
            return;
        }

        Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack stack : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), stack);
            }
        }

        String name = CustomItems.friendlyName(item);
        sender.sendMessage(Component.text("Выдан предмет «" + name + "» игроку " + target.getName() + ".", NamedTextColor.GREEN));
        if (!target.equals(sender)) {
            target.sendMessage(Component.text("Вы получили уникальный предмет «" + name + "»!", NamedTextColor.GREEN));
        }
    }

    private void list(CommandSender sender) {
        sender.sendMessage(Component.text("Уникальные предметы TwixRPG:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  ⛏ Аметистовая Кирка  —  pickaxe", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("  🪓 Лабрис Гефеста  —  axe", NamedTextColor.RED));
        sender.sendMessage(Component.text("  ⚔ Фарм-Меч  —  sword", NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("TwixRPG Items — команды:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  /twixitems give <pickaxe|axe|sword> [ник]  — выдать предмет", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /twixitems list  — список предметов", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("give", "list")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    result.add(option);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (String option : List.of("pickaxe", "axe", "sword")) {
                if (option.startsWith(args[1].toLowerCase())) {
                    result.add(option);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    result.add(player.getName());
                }
            }
        }
        return result;
    }
}
