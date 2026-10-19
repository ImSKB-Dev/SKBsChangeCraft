package com.advancedcraft.service;

import org.bukkit.entity.Player;

/**
 * Service for checking Bukkit permissions and evaluating OP bypass policies.
 */
public class PermissionService {

    private boolean opBypassPermissions = true;

    public PermissionService() {
    }

    public boolean isOpBypassPermissions() {
        return opBypassPermissions;
    }

    public void setOpBypassPermissions(boolean opBypassPermissions) {
        this.opBypassPermissions = opBypassPermissions;
    }

    public boolean hasPermission(Player player, String permission) {
        if (player == null) return false;
        if (permission == null || permission.isEmpty()) return true;

        if (opBypassPermissions && player.isOp()) {
            return true;
        }

        return player.hasPermission(permission);
    }
}
