package com.advancedcraft.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for lore editing on held ItemStacks using MiniMessage formatting.
 * Preserves existing item identity, metadata, display name, enchantments, etc.
 * Uses 1-based indexing for administrator usability.
 */
public class LoreService {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final String DEFAULT_SEPARATOR = "||";

    public boolean addLore(ItemStack item, String text) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<Component> currentLore = meta.lore();
        if (currentLore == null) {
            currentLore = new ArrayList<>();
        } else {
            currentLore = new ArrayList<>(currentLore);
        }

        List<String> lines = parseLines(text);
        for (String line : lines) {
            currentLore.add(miniMessage.deserialize(line));
        }

        meta.lore(currentLore);
        return item.setItemMeta(meta);
    }

    public boolean insertLore(ItemStack item, int lineNumber, String text) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<Component> currentLore = meta.lore();
        if (currentLore == null) {
            currentLore = new ArrayList<>();
        } else {
            currentLore = new ArrayList<>(currentLore);
        }

        if (lineNumber < 1 || lineNumber > currentLore.size() + 1) {
            return false;
        }

        List<String> lines = parseLines(text);
        int insertIndex = lineNumber - 1;
        for (String line : lines) {
            currentLore.add(insertIndex++, miniMessage.deserialize(line));
        }

        meta.lore(currentLore);
        return item.setItemMeta(meta);
    }

    public boolean setLore(ItemStack item, int lineNumber, String text) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<Component> currentLore = meta.lore();
        if (currentLore == null || currentLore.isEmpty()) {
            return false;
        } else {
            currentLore = new ArrayList<>(currentLore);
        }

        if (lineNumber < 1 || lineNumber > currentLore.size()) {
            return false;
        }

        List<String> lines = parseLines(text);
        int replaceIndex = lineNumber - 1;
        currentLore.set(replaceIndex, miniMessage.deserialize(lines.get(0)));
        for (int i = 1; i < lines.size(); i++) {
            currentLore.add(replaceIndex + i, miniMessage.deserialize(lines.get(i)));
        }

        meta.lore(currentLore);
        return item.setItemMeta(meta);
    }

    public boolean removeLore(ItemStack item, int lineNumber) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<Component> currentLore = meta.lore();
        if (currentLore == null || currentLore.isEmpty()) {
            return false;
        } else {
            currentLore = new ArrayList<>(currentLore);
        }

        if (lineNumber < 1 || lineNumber > currentLore.size()) {
            return false;
        }

        currentLore.remove(lineNumber - 1);
        meta.lore(currentLore.isEmpty() ? null : currentLore);
        return item.setItemMeta(meta);
    }

    public boolean clearLore(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        meta.lore(null);
        return item.setItemMeta(meta);
    }

    public boolean replaceLore(ItemStack item, String text) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<String> lines = parseLines(text);
        List<Component> newLore = new ArrayList<>();
        for (String line : lines) {
            newLore.add(miniMessage.deserialize(line));
        }

        meta.lore(newLore);
        return item.setItemMeta(meta);
    }

    public List<Component> getLore(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Collections.emptyList();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return Collections.emptyList();
        }
        return meta.lore();
    }

    private List<String> parseLines(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        if (text.contains(DEFAULT_SEPARATOR)) {
            String[] parts = text.split("\\|\\|");
            List<String> result = new ArrayList<>();
            for (String part : parts) {
                result.add(part.trim());
            }
            return result;
        }
        return List.of(text);
    }
}
