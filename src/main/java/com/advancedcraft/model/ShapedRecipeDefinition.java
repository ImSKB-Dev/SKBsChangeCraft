package com.advancedcraft.model;

import org.bukkit.Material;

import java.util.*;

/**
 * Data model for shaped recipes.
 */
public class ShapedRecipeDefinition {

    private final String id;
    private final List<String> shape;
    private final Map<Character, CustomIngredient> ingredients;
    private final CustomIngredient result;
    private final String permission;

    public ShapedRecipeDefinition(String id,
                                  List<String> shape,
                                  Map<Character, CustomIngredient> ingredients,
                                  CustomIngredient result,
                                  String permission) {
        this.id = Objects.requireNonNull(id, "Recipe ID cannot be null").toLowerCase(Locale.ROOT);
        this.shape = Objects.requireNonNull(shape, "Shape cannot be null");
        this.ingredients = Objects.requireNonNull(ingredients, "Ingredients cannot be null");
        this.result = Objects.requireNonNull(result, "Result cannot be null");
        this.permission = permission;
    }

    public String getId() {
        return id;
    }

    public List<String> getShape() {
        return Collections.unmodifiableList(shape);
    }

    public Map<Character, CustomIngredient> getIngredients() {
        return Collections.unmodifiableMap(ingredients);
    }

    public CustomIngredient getResult() {
        return result;
    }

    public String getPermission() {
        return permission;
    }
}
