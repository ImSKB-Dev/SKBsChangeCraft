package com.advancedcraft.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents a command action bound to a custom item.
 */
public class ItemActionDefinition {

    public enum TriggerType {
        RIGHT_CLICK,
        LEFT_CLICK,
        RIGHT_CLICK_BLOCK,
        INTERACT_ENTITY
    }

    public enum ExecutorMode {
        CONSOLE,
        PLAYER
    }

    public record CommandEntry(ExecutorMode executor, String command) {}

    private final String itemId;
    private final TriggerType trigger;
    private final String permission;
    private final int cooldownSeconds;
    private final boolean consumeItem;
    private final List<CommandEntry> commands;
    private final String noPermissionMessage;
    private final String cooldownMessage;

    public ItemActionDefinition(String itemId,
                                TriggerType trigger,
                                String permission,
                                int cooldownSeconds,
                                boolean consumeItem,
                                List<CommandEntry> commands,
                                String noPermissionMessage,
                                String cooldownMessage) {
        this.itemId = Objects.requireNonNull(itemId, "Item ID cannot be null").toLowerCase(Locale.ROOT);
        this.trigger = Objects.requireNonNull(trigger, "TriggerType cannot be null");
        this.permission = permission;
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.consumeItem = consumeItem;
        this.commands = commands != null ? new ArrayList<>(commands) : Collections.emptyList();
        this.noPermissionMessage = noPermissionMessage;
        this.cooldownMessage = cooldownMessage;
    }

    public String getItemId() {
        return itemId;
    }

    public TriggerType getTrigger() {
        return trigger;
    }

    public String getPermission() {
        return permission;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean isConsumeItem() {
        return consumeItem;
    }

    public List<CommandEntry> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    public String getCooldownMessage() {
        return cooldownMessage;
    }
}
