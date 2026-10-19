package com.advancedcraft.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents an ingredient in a custom crafting recipe.
 * Can be a vanilla Material or a custom item defined by internal ID.
 */
public class CustomIngredient {

    public enum MatchingMode {
        MATERIAL,
        CUSTOM_ITEM,
        STRICT_METADATA
    }

    private final MatchingMode mode;
    private final Material material;
    private final String customItemId;
    private final int amount;

    public CustomIngredient(MatchingMode mode, Material material, String customItemId, int amount) {
        this.mode = Objects.requireNonNull(mode, "MatchingMode cannot be null");
        this.material = material;
        this.customItemId = customItemId != null ? customItemId.toLowerCase(Locale.ROOT) : null;
        this.amount = Math.max(1, amount);
    }

    public static CustomIngredient ofMaterial(Material material, int amount) {
        return new CustomIngredient(MatchingMode.MATERIAL, material, null, amount);
    }

    public static CustomIngredient ofCustomItem(String customItemId, int amount) {
        return new CustomIngredient(MatchingMode.CUSTOM_ITEM, null, customItemId, amount);
    }

    public MatchingMode getMode() {
        return mode;
    }

    public Material getMaterial() {
        return material;
    }

    public String getCustomItemId() {
        return customItemId;
    }

    public int getAmount() {
        return amount;
    }
}
