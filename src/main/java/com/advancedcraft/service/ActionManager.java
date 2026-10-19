package com.advancedcraft.service;

import com.advancedcraft.model.ItemActionDefinition;
import com.advancedcraft.model.ItemActionDefinition.CommandEntry;
import com.advancedcraft.model.ItemActionDefinition.ExecutorMode;
import com.advancedcraft.model.ItemActionDefinition.TriggerType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service and listener for handling item-bound command actions, cooldowns, consumption, and execution.
 */
public class ActionManager implements Listener {

    private final Plugin plugin;
    private final Logger logger;
    private final ItemIdentifierService itemIdentifier;
    private final PermissionService permissionService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final Map<String, List<ItemActionDefinition>> itemActions = new HashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public ActionManager(Plugin plugin, ItemIdentifierService itemIdentifier, PermissionService permissionService) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.logger = plugin.getLogger();
        this.itemIdentifier = Objects.requireNonNull(itemIdentifier, "ItemIdentifierService cannot be null");
        this.permissionService = Objects.requireNonNull(permissionService, "PermissionService cannot be null");
    }

    public void clear() {
        itemActions.clear();
        cooldowns.clear();
    }

    public void registerAction(ItemActionDefinition action) {
        Objects.requireNonNull(action, "ItemActionDefinition cannot be null");
        itemActions.computeIfAbsent(action.getItemId(), k -> new ArrayList<>()).add(action);
    }

    public List<ItemActionDefinition> getActions(String itemId) {
        if (itemId == null) return Collections.emptyList();
        return itemActions.getOrDefault(itemId.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    public void loadActionsFromConfig(FileConfiguration config) {
        ConfigurationSection actionsSec = config.getConfigurationSection("item-actions");
        if (actionsSec == null) return;

        for (String itemId : actionsSec.getKeys(false)) {
            ConfigurationSection sec = actionsSec.getConfigurationSection(itemId);
            if (sec == null) continue;

            String trigStr = sec.getString("trigger", "RIGHT_CLICK").toUpperCase(Locale.ROOT);
            TriggerType trigger;
            try {
                trigger = TriggerType.valueOf(trigStr);
            } catch (IllegalArgumentException e) {
                logger.severe("Invalid trigger type '" + trigStr + "' for item-action: " + itemId);
                continue;
            }

            String perm = sec.getString("permission");
            int cooldown = sec.getInt("cooldown-seconds", 0);
            boolean consume = sec.getBoolean("consume-item", false);

            List<CommandEntry> commands = new ArrayList<>();
            List<Map<?, ?>> cmdList = sec.getMapList("commands");
            for (Map<?, ?> map : cmdList) {
                Object execObj = map.get("executor");
                ExecutorMode execMode = ExecutorMode.CONSOLE;
                if (execObj != null && "PLAYER".equalsIgnoreCase(execObj.toString())) {
                    execMode = ExecutorMode.PLAYER;
                }
                Object cmdObj = map.get("command");
                if (cmdObj != null) {
                    commands.add(new CommandEntry(execMode, cmdObj.toString()));
                }
            }

            String noPermMsg = sec.getString("messages.no-permission", "<red>You do not have permission to use this item.</red>");
            String cooldownMsg = sec.getString("messages.cooldown", "<yellow>Please wait before using this item again.</yellow>");

            ItemActionDefinition def = new ItemActionDefinition(
                    itemId, trigger, perm, cooldown, consume, commands, noPermMsg, cooldownMsg
            );

            registerAction(def);
            logger.info("Registered item action for item ID: " + itemId);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        Optional<String> customIdOpt = itemIdentifier.getCustomItemId(held);
        if (customIdOpt.isEmpty()) return;

        String itemId = customIdOpt.get();
        List<ItemActionDefinition> actions = getActions(itemId);
        if (actions.isEmpty()) return;

        Action act = event.getAction();
        TriggerType trigger = null;
        if (act == Action.RIGHT_CLICK_AIR || act == Action.RIGHT_CLICK_BLOCK) {
            trigger = (act == Action.RIGHT_CLICK_BLOCK) ? TriggerType.RIGHT_CLICK_BLOCK : TriggerType.RIGHT_CLICK;
        } else if (act == Action.LEFT_CLICK_AIR || act == Action.LEFT_CLICK_BLOCK) {
            trigger = TriggerType.LEFT_CLICK;
        }

        if (trigger == null) return;

        for (ItemActionDefinition actionDef : actions) {
            if (actionDef.getTrigger() == trigger || (actionDef.getTrigger() == TriggerType.RIGHT_CLICK && (trigger == TriggerType.RIGHT_CLICK_BLOCK || trigger == TriggerType.RIGHT_CLICK))) {
                executeAction(player, held, actionDef);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        Optional<String> customIdOpt = itemIdentifier.getCustomItemId(held);
        if (customIdOpt.isEmpty()) return;

        String itemId = customIdOpt.get();
        List<ItemActionDefinition> actions = getActions(itemId);

        for (ItemActionDefinition actionDef : actions) {
            if (actionDef.getTrigger() == TriggerType.INTERACT_ENTITY) {
                executeAction(player, held, actionDef);
            }
        }
    }

    private void executeAction(Player player, ItemStack item, ItemActionDefinition actionDef) {
        if (actionDef.getPermission() != null && !actionDef.getPermission().isEmpty()) {
            if (!permissionService.hasPermission(player, actionDef.getPermission())) {
                if (actionDef.getNoPermissionMessage() != null && !actionDef.getNoPermissionMessage().isEmpty()) {
                    player.sendMessage(miniMessage.deserialize(actionDef.getNoPermissionMessage()));
                }
                return;
            }
        }

        String cdKey = player.getUniqueId() + ":" + actionDef.getItemId() + ":" + actionDef.getTrigger();
        long now = System.currentTimeMillis();
        if (actionDef.getCooldownSeconds() > 0) {
            Long lastUse = cooldowns.get(cdKey);
            if (lastUse != null) {
                long elapsedSeconds = (now - lastUse) / 1000;
                if (elapsedSeconds < actionDef.getCooldownSeconds()) {
                    if (actionDef.getCooldownMessage() != null && !actionDef.getCooldownMessage().isEmpty()) {
                        long remaining = actionDef.getCooldownSeconds() - elapsedSeconds;
                        String msg = actionDef.getCooldownMessage().replace("{seconds}", String.valueOf(remaining));
                        player.sendMessage(miniMessage.deserialize(msg));
                    }
                    return;
                }
            }
        }

        for (CommandEntry entry : actionDef.getCommands()) {
            String processedCmd = entry.command()
                    .replace("{player}", player.getName())
                    .replace("{item_id}", actionDef.getItemId());

            if (entry.executor() == ExecutorMode.CONSOLE) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
            } else {
                player.performCommand(processedCmd);
            }
        }

        if (actionDef.getCooldownSeconds() > 0) {
            cooldowns.put(cdKey, now);
        }

        if (actionDef.isConsumeItem()) {
            item.setAmount(item.getAmount() - 1);
        }
    }
}
