package com.advancedcraft.command;

import com.advancedcraft.gui.CraftingGuiManager;
import com.advancedcraft.model.CustomItemDefinition;
import com.advancedcraft.model.ShapedRecipeDefinition;
import com.advancedcraft.model.ShapelessRecipeDefinition;
import com.advancedcraft.service.ItemIdentifierService;
import com.advancedcraft.service.LoreService;
import com.advancedcraft.service.PermissionService;
import com.advancedcraft.service.RecipeRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Administrative command executor and tab completer for /craft and /customcraft commands.
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final RecipeRegistry recipeRegistry;
    private final ItemIdentifierService itemIdentifier;
    private final PermissionService permissionService;
    private final CraftingGuiManager guiManager;
    private final LoreCommandExecutor loreCommandExecutor;
    private final Runnable reloadAction;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CommandManager(Plugin plugin,
                          RecipeRegistry recipeRegistry,
                          ItemIdentifierService itemIdentifier,
                          PermissionService permissionService,
                          CraftingGuiManager guiManager,
                          LoreService loreService,
                          Runnable reloadAction) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.recipeRegistry = Objects.requireNonNull(recipeRegistry, "RecipeRegistry cannot be null");
        this.itemIdentifier = Objects.requireNonNull(itemIdentifier, "ItemIdentifierService cannot be null");
        this.permissionService = Objects.requireNonNull(permissionService, "PermissionService cannot be null");
        this.guiManager = Objects.requireNonNull(guiManager, "CraftingGuiManager cannot be null");
        this.loreCommandExecutor = new LoreCommandExecutor(loreService);
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase(Locale.ROOT);

        if (cmdName.equals("craft")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console cannot open crafting GUI.", NamedTextColor.RED));
                return true;
            }
            if (!permissionService.hasPermission(player, "advancedcraft.use")) {
                player.sendMessage(Component.text("You do not have permission to open the crafting GUI.", NamedTextColor.RED));
                return true;
            }
            guiManager.openCraftingGui(player);
            return true;
        }

        if (cmdName.equals("customcraft")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }

            String sub = args[0].toLowerCase(Locale.ROOT);

            switch (sub) {
                case "menu" -> handleMenu(sender);
                case "give" -> handleGive(sender, args);
                case "remove" -> handleRemoveItem(sender, args);
                case "items" -> handleItems(sender);
                case "addrecipe" -> handleAddRecipe(sender, args);
                case "recipes" -> handleRecipes(sender);
                case "recipe" -> handleRecipeInfo(sender, args);
                case "reload" -> handleReload(sender);
                case "version" -> handleVersion(sender);
                case "lore" -> {
                    String[] loreArgs = Arrays.copyOfRange(args, 1, args.length);
                    loreCommandExecutor.onCommand(sender, command, label, loreArgs);
                }
                default -> sendHelp(sender);
            }
            return true;
        }

        return false;
    }

    private void handleMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Console cannot open crafting GUI.", NamedTextColor.RED));
            return;
        }
        if (!permissionService.hasPermission(player, "advancedcraft.use")) {
            player.sendMessage(Component.text("You do not have permission to open the crafting GUI.", NamedTextColor.RED));
            return;
        }
        guiManager.openCraftingGui(player);
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !permissionService.hasPermission(player, "advancedcraft.give")) {
            sender.sendMessage(Component.text("You do not have permission to give custom items.", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /customcraft give <player> <itemId> [amount]", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return;
        }

        String itemId = args[2].toLowerCase(Locale.ROOT);
        Optional<CustomItemDefinition> optDef = recipeRegistry.getCustomItem(itemId);
        if (optDef.isEmpty()) {
            sender.sendMessage(Component.text("Custom item not found: " + itemId, NamedTextColor.RED));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid amount: " + args[3], NamedTextColor.RED));
                return;
            }
        }

        ItemStack stack = itemIdentifier.createItemStack(optDef.get(), amount);
        HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), drop);
            }
        }

        sender.sendMessage(Component.text("Gave " + amount + "x " + itemId + " to " + target.getName() + ".", NamedTextColor.GREEN));
        target.sendMessage(Component.text("You received " + amount + "x " + itemId + ".", NamedTextColor.GREEN));
    }

    private void handleAddRecipe(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Console cannot save recipes from GUI grid.", NamedTextColor.RED));
            return;
        }

        if (!permissionService.hasPermission(player, "advancedcraft.addrecipe") && !permissionService.hasPermission(player, "advancedcraft.admin")) {
            player.sendMessage(Component.text("You do not have permission to add custom recipes.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /customcraft addrecipe <recipeId> [shaped|shapeless]", NamedTextColor.YELLOW));
            return;
        }

        String recipeId = args[1].toLowerCase(Locale.ROOT);
        String typeStr = args.length >= 3 ? args[2].toUpperCase(Locale.ROOT) : "SHAPED";

        if (!typeStr.equals("SHAPED") && !typeStr.equals("SHAPELESS")) {
            player.sendMessage(Component.text("Recipe type must be 'shaped' or 'shapeless'.", NamedTextColor.RED));
            return;
        }

        org.bukkit.inventory.Inventory topInv = player.getOpenInventory().getTopInventory();
        if (topInv == null || !guiManager.validateGui(topInv)) {
            player.sendMessage(Component.text("Please open the crafting GUI (/craft) and set up your grid and result item before running this command.", NamedTextColor.RED));
            return;
        }

        guiManager.saveRecipeFromGui(player, topInv, recipeId, typeStr);
    }

    private void handleRemoveItem(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !permissionService.hasPermission(player, "advancedcraft.remove")) {
            sender.sendMessage(Component.text("You do not have permission to remove custom items.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /customcraft remove <itemId>", NamedTextColor.YELLOW));
            return;
        }

        String itemId = args[1].toLowerCase(Locale.ROOT);
        boolean removed = recipeRegistry.removeCustomItem(itemId);
        if (removed) {
            sender.sendMessage(Component.text("Removed custom item: " + itemId, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Custom item not found: " + itemId, NamedTextColor.RED));
        }
    }

    private void handleItems(CommandSender sender) {
        if (sender instanceof Player player && !permissionService.hasPermission(player, "advancedcraft.items.list")) {
            sender.sendMessage(Component.text("You do not have permission to list custom items.", NamedTextColor.RED));
            return;
        }

        Map<String, CustomItemDefinition> items = recipeRegistry.getAllCustomItems();
        sender.sendMessage(Component.text("=== Registered Custom Items (" + items.size() + ") ===", NamedTextColor.GOLD));
        for (CustomItemDefinition item : items.values()) {
            sender.sendMessage(Component.text("- " + item.getId() + " (" + item.getMaterial() + ")", NamedTextColor.YELLOW));
        }
    }

    private void handleRecipes(CommandSender sender) {
        if (sender instanceof Player player && !permissionService.hasPermission(player, "advancedcraft.recipes.list")) {
            sender.sendMessage(Component.text("You do not have permission to list custom recipes.", NamedTextColor.RED));
            return;
        }

        Map<String, ShapedRecipeDefinition> shaped = recipeRegistry.getShapedRecipes();
        Map<String, ShapelessRecipeDefinition> shapeless = recipeRegistry.getShapelessRecipes();

        sender.sendMessage(Component.text("=== Registered Recipes (" + (shaped.size() + shapeless.size()) + ") ===", NamedTextColor.GOLD));
        for (ShapedRecipeDefinition r : shaped.values()) {
            sender.sendMessage(Component.text("- " + r.getId() + " (SHAPED)", NamedTextColor.YELLOW));
        }
        for (ShapelessRecipeDefinition r : shapeless.values()) {
            sender.sendMessage(Component.text("- " + r.getId() + " (SHAPELESS)", NamedTextColor.YELLOW));
        }
    }

    private void handleRecipeInfo(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !permissionService.hasPermission(player, "advancedcraft.recipes.list")) {
            sender.sendMessage(Component.text("You do not have permission to view recipe info.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /customcraft recipe <recipeId>", NamedTextColor.YELLOW));
            return;
        }

        String recipeId = args[1].toLowerCase(Locale.ROOT);
        ShapedRecipeDefinition shaped = recipeRegistry.getShapedRecipes().get(recipeId);
        if (shaped != null) {
            sender.sendMessage(Component.text("=== Recipe Info: " + recipeId + " (SHAPED) ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Permission: " + (shaped.getPermission() != null ? shaped.getPermission() : "None"), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Shape: " + shaped.getShape(), NamedTextColor.YELLOW));
            return;
        }

        ShapelessRecipeDefinition shapeless = recipeRegistry.getShapelessRecipes().get(recipeId);
        if (shapeless != null) {
            sender.sendMessage(Component.text("=== Recipe Info: " + recipeId + " (SHAPELESS) ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Permission: " + (shapeless.getPermission() != null ? shapeless.getPermission() : "None"), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Ingredients count: " + shapeless.getIngredients().size(), NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("Recipe not found: " + recipeId, NamedTextColor.RED));
    }

    private void handleReload(CommandSender sender) {
        if (sender instanceof Player player && !permissionService.hasPermission(player, "advancedcraft.reload")) {
            sender.sendMessage(Component.text("You do not have permission to reload configuration.", NamedTextColor.RED));
            return;
        }

        if (reloadAction != null) {
            reloadAction.run();
            sender.sendMessage(Component.text("AdvancedCraft configuration reloaded successfully.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Reload action unavailable.", NamedTextColor.RED));
        }
    }

    private void handleVersion(CommandSender sender) {
        sender.sendMessage(Component.text("AdvancedCraft v" + plugin.getPluginMeta().getVersion() + " (Paper 1.21.4 API)", NamedTextColor.GOLD));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== AdvancedCraft Help ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/craft or /customcraft menu - Open Crafting GUI", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/customcraft addrecipe <recipeId> [shaped|shapeless] - Save active GUI grid as a recipe", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/customcraft give <player> <itemId> [amount] - Give custom item", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/customcraft remove <itemId> - Remove registered custom item", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/customcraft items - List registered custom items", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/customcraft recipes - List registered recipes", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/customcraft recipe <recipeId> - Show recipe details", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/customcraft lore ... - Manage held item lore", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/customcraft reload - Reload configurations", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/customcraft version - Display plugin version", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase(Locale.ROOT);
        if (cmdName.equals("craft")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subcommands = List.of("menu", "addrecipe", "give", "remove", "items", "recipes", "recipe", "reload", "version", "lore");
            return filterPrefix(subcommands, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("give")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                return filterPrefix(players, args[1]);
            } else if (sub.equals("remove")) {
                List<String> items = new ArrayList<>(recipeRegistry.getAllCustomItems().keySet());
                return filterPrefix(items, args[1]);
            } else if (sub.equals("recipe")) {
                List<String> recipes = new ArrayList<>();
                recipes.addAll(recipeRegistry.getShapedRecipes().keySet());
                recipes.addAll(recipeRegistry.getShapelessRecipes().keySet());
                return filterPrefix(recipes, args[1]);
            } else if (sub.equals("addrecipe")) {
                return Collections.emptyList();
            } else if (sub.equals("lore")) {
                return loreCommandExecutor.onTabComplete(sender, command, alias, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("addrecipe")) {
            return filterPrefix(List.of("shaped", "shapeless"), args[2]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> items = new ArrayList<>(recipeRegistry.getAllCustomItems().keySet());
            return filterPrefix(items, args[2]);
        }

        if (args[0].equalsIgnoreCase("lore")) {
            return loreCommandExecutor.onTabComplete(sender, command, alias, Arrays.copyOfRange(args, 1, args.length));
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
