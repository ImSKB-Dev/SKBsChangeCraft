package com.advancedcraft;

import com.advancedcraft.command.CommandManager;
import com.advancedcraft.gui.CraftingGuiManager;
import com.advancedcraft.service.ActionManager;
import com.advancedcraft.service.ItemIdentifierService;
import com.advancedcraft.service.LoreService;
import com.advancedcraft.service.PermissionService;
import com.advancedcraft.service.RecipeRegistry;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Main JavaPlugin lifecycle manager for AdvancedCraft.
 */
public class AdvancedCraftPlugin extends JavaPlugin {

    private ItemIdentifierService itemIdentifier;
    private PermissionService permissionService;
    private RecipeRegistry recipeRegistry;
    private LoreService loreService;
    private CraftingGuiManager guiManager;
    private ActionManager actionManager;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        saveDefaultResourceFiles();

        this.itemIdentifier = new ItemIdentifierService(this);
        this.permissionService = new PermissionService();
        this.recipeRegistry = new RecipeRegistry(this, itemIdentifier);
        this.loreService = new LoreService();
        this.guiManager = new CraftingGuiManager(this, recipeRegistry, itemIdentifier);
        this.actionManager = new ActionManager(this, itemIdentifier, permissionService);

        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(actionManager, this);

        reloadPluginConfigurations();

        this.commandManager = new CommandManager(
                this, recipeRegistry, itemIdentifier, permissionService, guiManager, loreService, this::reloadPluginConfigurations
        );

        PluginCommand craftCmd = getCommand("craft");
        if (craftCmd != null) {
            craftCmd.setExecutor(commandManager);
            craftCmd.setTabCompleter(commandManager);
        }

        PluginCommand customCraftCmd = getCommand("customcraft");
        if (customCraftCmd != null) {
            customCraftCmd.setExecutor(commandManager);
            customCraftCmd.setTabCompleter(commandManager);
        }

        getLogger().info("AdvancedCraft 1.0.0 enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (guiManager != null) {
            guiManager.closeAllSessions();
        }
        if (recipeRegistry != null) {
            recipeRegistry.clear();
        }
        if (actionManager != null) {
            actionManager.clear();
        }
        getLogger().info("AdvancedCraft disabled.");
    }

    public void reloadPluginConfigurations() {
        reloadConfig();
        saveDefaultResourceFiles();

        boolean opBypass = getConfig().getBoolean("settings.op-bypass-permissions", true);
        permissionService.setOpBypassPermissions(opBypass);

        File guiFile = new File(getDataFolder(), "gui.yml");
        if (guiFile.exists()) {
            YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
            int resSlot = guiConfig.getInt("result-slot", 24);
            guiManager.setResultSlot(resSlot);
        }

        recipeRegistry.clear();
        actionManager.clear();

        File itemsFile = new File(getDataFolder(), "items.yml");
        if (itemsFile.exists()) {
            YamlConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
            recipeRegistry.loadItemsFromConfig(itemsConfig);
        }

        File recipesFile = new File(getDataFolder(), "recipes.yml");
        if (recipesFile.exists()) {
            YamlConfiguration recipesConfig = YamlConfiguration.loadConfiguration(recipesFile);
            recipeRegistry.loadRecipesFromConfig(recipesConfig);
        }

        File actionsFile = new File(getDataFolder(), "item-actions.yml");
        if (actionsFile.exists()) {
            YamlConfiguration actionsConfig = YamlConfiguration.loadConfiguration(actionsFile);
            actionManager.loadActionsFromConfig(actionsConfig);
        }
    }

    private void saveDefaultResourceFiles() {
        saveDefaultConfig();
        saveResourceIfNotExists("messages.yml");
        saveResourceIfNotExists("items.yml");
        saveResourceIfNotExists("recipes.yml");
        saveResourceIfNotExists("item-actions.yml");
        saveResourceIfNotExists("gui.yml");
        saveResourceIfNotExists("README.txt");
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }

    public RecipeRegistry getRecipeRegistry() {
        return recipeRegistry;
    }

    public ItemIdentifierService getItemIdentifier() {
        return itemIdentifier;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public LoreService getLoreService() {
        return loreService;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }
}
