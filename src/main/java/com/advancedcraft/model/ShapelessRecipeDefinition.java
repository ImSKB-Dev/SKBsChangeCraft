package com.advancedcraft.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Data model for shapeless recipes.
 */
public class ShapelessRecipeDefinition {

    private final String id;
    private final List<CustomIngredient> ingredients;
    private final CustomIngredient result;
    private final String permission;

    public ShapelessRecipeDefinition(String id,
                                    List<CustomIngredient> ingredients,
                                    CustomIngredient result,
                                    String permission) {
        this.id = Objects.requireNonNull(id, "Recipe ID cannot be null").toLowerCase(Locale.ROOT);
        this.ingredients = ingredients != null ? new ArrayList<>(ingredients) : new ArrayList<>();
        this.result = Objects.requireNonNull(result, "Result cannot be null");
        this.permission = permission;
    }

    public String getId() {
        return id;
    }

    public List<CustomIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public CustomIngredient getResult() {
        return result;
    }

    public String getPermission() {
        return permission;
    }
}
