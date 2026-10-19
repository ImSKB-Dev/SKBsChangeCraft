package com.advancedcraft.service;

import com.advancedcraft.model.CustomIngredient;
import com.advancedcraft.model.CustomItemDefinition;
import com.advancedcraft.model.ShapedRecipeDefinition;
import com.advancedcraft.model.ShapelessRecipeDefinition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * Registry for managing custom items and custom recipes.
 * Supports ingredient validation and recipe parsing from YAML configurations.
 */
public class RecipeRegistry {

    private final Plugin plugin;
    private final Logger logger;
    private final ItemIdentifierService itemIdentifier;

    private final Map<String, CustomItemDefinition> customItems = new HashMap<>();
    private final Map<String, ShapedRecipeDefinition> shapedRecipes = new HashMap<>();
    private final Map<String, ShapelessRecipeDefinition> shapelessRecipes = new HashMap<>();

    public RecipeRegistry(Plugin plugin, ItemIdentifierService itemIdentifier) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.logger = plugin.getLogger();
        this.itemIdentifier = Objects.requireNonNull(itemIdentifier, "ItemIdentifierService cannot be null");
    }

    public void clear() {
        customItems.clear();
        shapedRecipes.clear();
        shapelessRecipes.clear();
    }

    public void registerCustomItem(CustomItemDefinition item) {
        Objects.requireNonNull(item, "CustomItemDefinition cannot be null");
        customItems.put(item.getId(), item);
    }

    public boolean removeCustomItem(String id) {
        if (id == null) return false;
        return customItems.remove(id.toLowerCase(Locale.ROOT)) != null;
    }

    public Optional<CustomItemDefinition> getCustomItem(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(customItems.get(id.toLowerCase(Locale.ROOT)));
    }

    public Map<String, CustomItemDefinition> getAllCustomItems() {
        return Collections.unmodifiableMap(customItems);
    }

    public void registerShapedRecipe(ShapedRecipeDefinition recipe) {
        Objects.requireNonNull(recipe, "ShapedRecipeDefinition cannot be null");
        shapedRecipes.put(recipe.getId(), recipe);
    }

    public void registerShapelessRecipe(ShapelessRecipeDefinition recipe) {
        Objects.requireNonNull(recipe, "ShapelessRecipeDefinition cannot be null");
        shapelessRecipes.put(recipe.getId(), recipe);
    }

    public Map<String, ShapedRecipeDefinition> getShapedRecipes() {
        return Collections.unmodifiableMap(shapedRecipes);
    }

    public Map<String, ShapelessRecipeDefinition> getShapelessRecipes() {
        return Collections.unmodifiableMap(shapelessRecipes);
    }

    public ItemStack createIngredientItemStack(CustomIngredient ingredient) {
        if (ingredient == null) return null;
        if (ingredient.getMode() == CustomIngredient.MatchingMode.CUSTOM_ITEM) {
            CustomItemDefinition def = customItems.get(ingredient.getCustomItemId());
            if (def != null) {
                return itemIdentifier.createItemStack(def, ingredient.getAmount());
            } else {
                logger.warning("Custom item ID not found for ingredient: " + ingredient.getCustomItemId());
                return null;
            }
        } else {
            if (ingredient.getMaterial() != null) {
                return new ItemStack(ingredient.getMaterial(), ingredient.getAmount());
            }
        }
        return null;
    }

    public boolean matchesIngredient(ItemStack provided, CustomIngredient required) {
        if (required == null) {
            return provided == null || provided.getType().isAir();
        }
        if (provided == null || provided.getType().isAir()) {
            return false;
        }

        if (provided.getAmount() < required.getAmount()) {
            return false;
        }

        if (required.getMode() == CustomIngredient.MatchingMode.CUSTOM_ITEM) {
            Optional<String> customId = itemIdentifier.getCustomItemId(provided);
            if (customId.isEmpty()) {
                return false;
            }
            return customId.get().equalsIgnoreCase(required.getCustomItemId());
        } else {
            Optional<String> customId = itemIdentifier.getCustomItemId(provided);
            if (customId.isPresent()) {
                return false;
            }
            return provided.getType() == required.getMaterial();
        }
    }

    public void loadItemsFromConfig(FileConfiguration config) {
        ConfigurationSection itemsSec = config.getConfigurationSection("items");
        if (itemsSec == null) return;

        for (String id : itemsSec.getKeys(false)) {
            ConfigurationSection sec = itemsSec.getConfigurationSection(id);
            if (sec == null) continue;

            String matStr = sec.getString("material");
            if (matStr == null) {
                logger.severe("Item '" + id + "' is missing 'material' property.");
                continue;
            }
            Material material = Material.matchMaterial(matStr);
            if (material == null) {
                logger.severe("Item '" + id + "' has invalid material: " + matStr);
                continue;
            }

            String name = sec.getString("name");
            List<String> lore = sec.getStringList("lore");
            Integer customModelData = sec.contains("custom-model-data") ? sec.getInt("custom-model-data") : null;
            boolean unbreakable = sec.getBoolean("unbreakable", false);
            Integer maxStackSize = sec.contains("max-stack-size") ? sec.getInt("max-stack-size") : null;
            Integer durability = sec.contains("durability") ? sec.getInt("durability") : null;

            Map<Enchantment, Integer> enchantments = new HashMap<>();
            ConfigurationSection enchantsSec = sec.getConfigurationSection("enchantments");
            if (enchantsSec != null) {
                for (String enchantKey : enchantsSec.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey.toLowerCase(Locale.ROOT)));
                    if (enchantment != null) {
                        enchantments.put(enchantment, enchantsSec.getInt(enchantKey, 1));
                    } else {
                        logger.warning("Unknown enchantment '" + enchantKey + "' on item " + id);
                    }
                }
            }

            Set<ItemFlag> itemFlags = EnumSet.noneOf(ItemFlag.class);
            List<String> flagList = sec.getStringList("item-flags");
            for (String flagStr : flagList) {
                try {
                    itemFlags.add(ItemFlag.valueOf(flagStr.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    logger.warning("Unknown item flag '" + flagStr + "' on item " + id);
                }
            }

            String obtainPerm = sec.getString("permissions.obtain");
            String usePerm = sec.getString("permissions.use");

            CustomItemDefinition def = new CustomItemDefinition(
                    id, material, name, lore, customModelData,
                    enchantments, itemFlags,
                    unbreakable, maxStackSize, durability,
                    Collections.emptyMap(), Collections.emptyMap(),
                    obtainPerm, usePerm
            );

            registerCustomItem(def);
            logger.info("Registered custom item: " + id);
        }
    }

    public void loadRecipesFromConfig(FileConfiguration config) {
        ConfigurationSection recipesSec = config.getConfigurationSection("recipes");
        if (recipesSec == null) return;

        for (String id : recipesSec.getKeys(false)) {
            ConfigurationSection sec = recipesSec.getConfigurationSection(id);
            if (sec == null) continue;

            String typeStr = sec.getString("type", "SHAPED").toUpperCase(Locale.ROOT);
            String permission = sec.getString("permission");

            ConfigurationSection resultSec = sec.getConfigurationSection("result");
            if (resultSec == null) {
                logger.severe("Recipe '" + id + "' is missing 'result' section.");
                continue;
            }
            CustomIngredient result = parseIngredient(resultSec, id, "result");
            if (result == null) continue;

            if ("SHAPED".equals(typeStr)) {
                List<String> shape = sec.getStringList("shape");
                if (shape.isEmpty()) {
                    logger.severe("Shaped recipe '" + id + "' missing 'shape' definition.");
                    continue;
                }

                ConfigurationSection ingMapSec = sec.getConfigurationSection("ingredients");
                if (ingMapSec == null) {
                    logger.severe("Shaped recipe '" + id + "' missing 'ingredients' section.");
                    continue;
                }

                Map<Character, CustomIngredient> ingredients = new HashMap<>();
                boolean valid = true;
                for (String key : ingMapSec.getKeys(false)) {
                    if (key.length() != 1) {
                        logger.severe("Shaped recipe '" + id + "' ingredient key must be single char, got: " + key);
                        valid = false;
                        break;
                    }
                    char c = key.charAt(0);
                    ConfigurationSection ingSec = ingMapSec.getConfigurationSection(key);
                    CustomIngredient ing = parseIngredient(ingSec, id, "ingredient " + c);
                    if (ing == null) {
                        valid = false;
                        break;
                    }
                    ingredients.put(c, ing);
                }

                if (valid) {
                    ShapedRecipeDefinition recipe = new ShapedRecipeDefinition(id, shape, ingredients, result, permission);
                    registerShapedRecipe(recipe);
                    logger.info("Registered shaped recipe: " + id);
                }

            } else if ("SHAPELESS".equals(typeStr)) {
                List<Map<?, ?>> ingList = sec.getMapList("ingredients");
                if (ingList.isEmpty()) {
                    logger.severe("Shapeless recipe '" + id + "' missing 'ingredients' list.");
                    continue;
                }

                List<CustomIngredient> ingredients = new ArrayList<>();
                boolean valid = true;
                for (Map<?, ?> map : ingList) {
                    CustomIngredient ing = parseIngredientMap(map, id);
                    if (ing == null) {
                        valid = false;
                        break;
                    }
                    ingredients.add(ing);
                }

                if (valid) {
                    ShapelessRecipeDefinition recipe = new ShapelessRecipeDefinition(id, ingredients, result, permission);
                    registerShapelessRecipe(recipe);
                    logger.info("Registered shapeless recipe: " + id);
                }
            } else {
                logger.severe("Recipe '" + id + "' has unknown type: " + typeStr);
            }
        }
    }

    private CustomIngredient parseIngredient(ConfigurationSection sec, String recipeId, String context) {
        if (sec == null) {
            logger.severe("Recipe '" + recipeId + "' " + context + " configuration section is missing.");
            return null;
        }
        String type = sec.getString("type", "MATERIAL").toUpperCase(Locale.ROOT);
        int amount = sec.getInt("amount", 1);

        if ("CUSTOM_ITEM".equals(type)) {
            String itemId = sec.getString("id");
            if (itemId == null) {
                logger.severe("Recipe '" + recipeId + "' " + context + " specifies CUSTOM_ITEM but missing 'id'.");
                return null;
            }
            return CustomIngredient.ofCustomItem(itemId, amount);
        } else {
            String matStr = sec.getString("material");
            if (matStr == null) {
                logger.severe("Recipe '" + recipeId + "' " + context + " specifies MATERIAL but missing 'material'.");
                return null;
            }
            Material material = Material.matchMaterial(matStr);
            if (material == null) {
                logger.severe("Recipe '" + recipeId + "' " + context + " has invalid material: " + matStr);
                return null;
            }
            return CustomIngredient.ofMaterial(material, amount);
        }
    }

    private CustomIngredient parseIngredientMap(Map<?, ?> map, String recipeId) {
        Object typeObj = map.get("type");
        String type = typeObj != null ? typeObj.toString().toUpperCase(Locale.ROOT) : "MATERIAL";
        Object amountObj = map.get("amount");
        int amount = amountObj instanceof Number num ? num.intValue() : 1;

        if ("CUSTOM_ITEM".equals(type)) {
            Object idObj = map.get("id");
            if (idObj == null) {
                logger.severe("Shapeless recipe '" + recipeId + "' ingredient specifies CUSTOM_ITEM but missing 'id'.");
                return null;
            }
            return CustomIngredient.ofCustomItem(idObj.toString(), amount);
        } else {
            Object matObj = map.get("material");
            if (matObj == null) {
                logger.severe("Shapeless recipe '" + recipeId + "' ingredient specifies MATERIAL but missing 'material'.");
                return null;
            }
            Material material = Material.matchMaterial(matObj.toString());
            if (material == null) {
                logger.severe("Shapeless recipe '" + recipeId + "' ingredient has invalid material: " + matObj);
                return null;
            }
            return CustomIngredient.ofMaterial(material, amount);
        }
    }
}
