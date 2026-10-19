package com.advancedcraft.command;

import com.advancedcraft.service.LoreService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Executor and TabCompleter for /customcraft lore operations.
 */
public class LoreCommandExecutor implements CommandExecutor, TabCompleter {

    private final LoreService loreService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public LoreCommandExecutor(LoreService loreService) {
        this.loreService = Objects.requireNonNull(loreService, "LoreService cannot be null");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Console cannot edit held item lore. Use this command in-game.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendLoreHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        ItemStack held = player.getInventory().getItemInMainHand();

        if (sub.equals("add")) {
            handleAdd(player, held, args);
        } else if (sub.equals("insert")) {
            handleInsert(player, held, args);
        } else if (sub.equals("set")) {
            handleSet(player, held, args);
        } else if (sub.equals("remove")) {
            handleRemove(player, held, args);
        } else if (sub.equals("clear")) {
            handleClear(player, held);
        } else if (sub.equals("show")) {
            handleShow(player, held);
        } else if (sub.equals("replace")) {
            handleReplace(player, held, args);
        } else {
            sendLoreHelp(player);
        }

        return true;
    }

    private boolean checkMainHandItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            player.sendMessage(Component.text("You must be holding an item in your main hand to edit lore.", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private boolean checkPermission(Player player, String perm) {
        if (!player.hasPermission(perm) && !player.hasPermission("advancedcraft.lore")) {
            player.sendMessage(Component.text("You do not have permission to use this lore command.", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private void handleAdd(Player player, ItemStack item, String[] args) {
        if (!checkPermission(player, "advancedcraft.lore.add")) return;
        if (!checkMainHandItem(player, item)) return;

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /customcraft lore add <text>", NamedTextColor.YELLOW));
            return;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        boolean success = loreService.addLore(item, text);
        if (success) {
            player.sendMessage(Component.text("Added line(s) to item lore.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to add lore. Make sure you hold a valid item.", NamedTextColor.RED));
        }
    }

    private void handleInsert(Player player, ItemStack item, String[] args) {
        if (!checkPermission(player, "advancedcraft.lore.insert")) return;
        if (!checkMainHandItem(player, item)) return;

        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /customcraft lore insert <line> <text>", NamedTextColor.YELLOW));
            return;
        }

        int lineNum;
        try {
            lineNum = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid line number: " + args[1], NamedTextColor.RED));
            return;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        boolean success = loreService.insertLore(item, lineNum, text);
        if (success) {
            player.sendMessage(Component.text("Inserted line(s) at position " + lineNum + ".", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to insert lore line. Check the line number.", NamedTextColor.RED));
        }
    }

    private void handleSet(Player player, ItemStack item, String[] args) {
        if (!checkPermission(player, "advancedcraft.lore.set")) return;
        if (!checkMainHandItem(player, item)) return;

        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /customcraft lore set <line> <text>", NamedTextColor.YELLOW));
            return;
        }

        int lineNum;
        try {
            lineNum = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid line number: " + args[1], NamedTextColor.RED));
            return;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        boolean success = loreService.setLore(item, lineNum, text);
        if (success) {
            player.sendMessage(Component.text("Replaced lore line " + lineNum + ".", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to replace lore line. Check the line number.", NamedTextColor.RED));
        }
    }

    private void handleRemove(Player player, ItemStack item, String[] args) {
        if (!checkPermission(player, "advancedcraft.lore.remove")) return;
        if (!checkMainHandItem(player, item)) return;

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /customcraft lore remove <line>", NamedTextColor.YELLOW));
            return;
        }

        int lineNum;
        try {
            lineNum = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid line number: " + args[1], NamedTextColor.RED));
            return;
        }

        boolean success = loreService.removeLore(item, lineNum);
        if (success) {
            player.sendMessage(Component.text("Removed lore line " + lineNum + ".", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to remove lore line. Check the line number.", NamedTextColor.RED));
        }
    }

    private void handleClear(Player player, ItemStack item) {
        if (!checkPermission(player, "advancedcraft.lore.clear")) return;
        if (!checkMainHandItem(player, item)) return;

        boolean success = loreService.clearLore(item);
        if (success) {
            player.sendMessage(Component.text("Cleared all lore lines.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to clear lore.", NamedTextColor.RED));
        }
    }

    private void handleShow(Player player, ItemStack item) {
        if (!checkPermission(player, "advancedcraft.lore.show")) return;
        if (!checkMainHandItem(player, item)) return;

        List<Component> lore = loreService.getLore(item);
        if (lore.isEmpty()) {
            player.sendMessage(Component.text("This item has no lore.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("=== Current Item Lore ===", NamedTextColor.GOLD));
        for (int i = 0; i < lore.size(); i++) {
            Component lineNum = Component.text((i + 1) + ": ", NamedTextColor.GRAY);
            player.sendMessage(lineNum.append(lore.get(i)));
        }
    }

    private void handleReplace(Player player, ItemStack item, String[] args) {
        if (!checkPermission(player, "advancedcraft.lore.replace")) return;
        if (!checkMainHandItem(player, item)) return;

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /customcraft lore replace <text>", NamedTextColor.YELLOW));
            return;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        boolean success = loreService.replaceLore(item, text);
        if (success) {
            player.sendMessage(Component.text("Replaced entire item lore.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to replace lore.", NamedTextColor.RED));
        }
    }

    private void sendLoreHelp(Player player) {
        player.sendMessage(Component.text("=== AdvancedCraft Lore Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/customcraft lore add <text> - Append lore line(s)", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/customcraft lore insert <line> <text> - Insert lore line(s)", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/customcraft lore set <line> <text> - Replace a specific line", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/customcraft lore remove <line> - Remove a specific line", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/customcraft lore clear - Clear all lore lines", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/customcraft lore show - Display current lore lines", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/customcraft lore replace <text> - Replace all lore", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = List.of("add", "insert", "set", "remove", "clear", "show", "replace");
            return filterPrefix(subcommands, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(s);
            }
        }
        return matches;
    }
}
