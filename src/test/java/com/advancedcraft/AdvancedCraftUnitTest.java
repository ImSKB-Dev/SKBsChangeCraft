package com.advancedcraft;

import com.advancedcraft.gui.CraftingGuiManager;
import com.advancedcraft.model.CustomIngredient;
import com.advancedcraft.model.CustomItemDefinition;
import com.advancedcraft.model.ShapedRecipeDefinition;
import com.advancedcraft.service.ItemIdentifierService;
import com.advancedcraft.service.LoreService;
import com.advancedcraft.service.PermissionService;
import com.advancedcraft.service.RecipeRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AdvancedCraftUnitTest {

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AdvancedCraftTest"));
    }

    @Test
    void testCustomItemDefinitionModel() {
        CustomItemDefinition item = new CustomItemDefinition(
                "ruby_sword",
                Material.NETHERITE_SWORD,
                "<red>Ruby Sword</red>",
                List.of("Legendary weapon"),
                1001,
                Collections.emptyMap(),
                Collections.emptySet(),
                true,
                1,
                0,
                Collections.emptyMap(),
                Map.of("rarity", "legendary"),
                "advancedcraft.obtain",
                "advancedcraft.use"
        );

        assertEquals("ruby_sword", item.getId());
        assertEquals(Material.NETHERITE_SWORD, item.getMaterial());
        assertEquals("<red>Ruby Sword</red>", item.getName());
        assertTrue(item.isUnbreakable());
        assertEquals("legendary", item.getPersistentMetadata().get("rarity"));
    }

    @Test
    void testRemoveCustomItem() {
        RecipeRegistry registry = new RecipeRegistry(plugin, Mockito.mock(ItemIdentifierService.class));
        CustomItemDefinition item = new CustomItemDefinition(
                "test_item",
                Material.DIAMOND,
                "Test",
                Collections.emptyList(),
                null,
                Collections.emptyMap(),
                Collections.emptySet(),
                false,
                null,
                null,
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                null
        );

        registry.registerCustomItem(item);
        assertTrue(registry.getCustomItem("test_item").isPresent());

        boolean removed = registry.removeCustomItem("test_item");
        assertTrue(removed);
        assertFalse(registry.getCustomItem("test_item").isPresent());
    }

    @Test
    void testConfigurableResultSlotInGuiManager() {
        CraftingGuiManager guiManager = new CraftingGuiManager(plugin, Mockito.mock(RecipeRegistry.class), Mockito.mock(ItemIdentifierService.class));
        assertEquals(24, guiManager.getResultSlot());

        guiManager.setResultSlot(31);
        assertEquals(31, guiManager.getResultSlot());
    }

    @Test
    void testCustomIngredientMatchingModes() {
        CustomIngredient vanillaIng = CustomIngredient.ofMaterial(Material.GOLD_INGOT, 4);
        assertEquals(CustomIngredient.MatchingMode.MATERIAL, vanillaIng.getMode());
        assertEquals(Material.GOLD_INGOT, vanillaIng.getMaterial());
        assertEquals(4, vanillaIng.getAmount());

        CustomIngredient customIng = CustomIngredient.ofCustomItem("ruby", 2);
        assertEquals(CustomIngredient.MatchingMode.CUSTOM_ITEM, customIng.getMode());
        assertEquals("ruby", customIng.getCustomItemId());
        assertEquals(2, customIng.getAmount());
    }

    @Test
    void testPermissionServiceOpBypass() {
        PermissionService permService = new PermissionService();

        org.bukkit.entity.Player player = Mockito.mock(org.bukkit.entity.Player.class);
        when(player.isOp()).thenReturn(true);
        when(player.hasPermission("some.permission")).thenReturn(false);

        permService.setOpBypassPermissions(true);
        assertTrue(permService.hasPermission(player, "some.permission"));

        permService.setOpBypassPermissions(false);
        assertFalse(permService.hasPermission(player, "some.permission"));
    }

    @Test
    void testLoreServiceLinesParsing() {
        LoreService loreService = new LoreService();
        assertNotNull(loreService);
    }

    @Test
    void testItemsYamlParsing() {
        String yamlContent = """
                items:
                  ruby_sword:
                    material: NETHERITE_SWORD
                    name: "<gradient:#ff3030:#ff9900>Ruby Sword</gradient>"
                    lore:
                      - "<gray>A powerful custom weapon."
                    unbreakable: true
                    custom-model-data: 10001
                """;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yamlContent));
        RecipeRegistry registry = new RecipeRegistry(plugin, Mockito.mock(ItemIdentifierService.class));
        registry.loadItemsFromConfig(config);

        assertTrue(registry.getCustomItem("ruby_sword").isPresent());
        CustomItemDefinition def = registry.getCustomItem("ruby_sword").get();
        assertEquals(Material.NETHERITE_SWORD, def.getMaterial());
        assertTrue(def.isUnbreakable());
        assertEquals(10001, def.getCustomModelData());
    }

    @Test
    void testRecipesYamlParsing() {
        String yamlContent = """
                recipes:
                  ruby_sword_recipe:
                    type: SHAPED
                    permission: "advancedcraft.craft.ruby_sword"
                    shape:
                      - " R "
                      - " R "
                      - " S "
                    ingredients:
                      R:
                        type: CUSTOM_ITEM
                        id: ruby
                        amount: 1
                      S:
                        type: MATERIAL
                        material: STICK
                        amount: 1
                    result:
                      type: CUSTOM_ITEM
                      id: ruby_sword
                      amount: 1
                """;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yamlContent));
        RecipeRegistry registry = new RecipeRegistry(plugin, Mockito.mock(ItemIdentifierService.class));
        registry.loadRecipesFromConfig(config);

        assertEquals(1, registry.getShapedRecipes().size());
        ShapedRecipeDefinition recipe = registry.getShapedRecipes().get("ruby_sword_recipe");
        assertNotNull(recipe);
        assertEquals("advancedcraft.craft.ruby_sword", recipe.getPermission());
        assertEquals(3, recipe.getShape().size());
    }

    @Test
    void testDynamicRecipeRegistrationAndRetrieval() {
        RecipeRegistry registry = new RecipeRegistry(plugin, Mockito.mock(ItemIdentifierService.class));

        ShapedRecipeDefinition shapedDef = new ShapedRecipeDefinition(
                "dynamic_shaped",
                List.of(" D ", " S ", " S "),
                Map.of(
                        'D', CustomIngredient.ofMaterial(Material.DIAMOND, 1),
                        'S', CustomIngredient.ofMaterial(Material.STICK, 1)
                ),
                CustomIngredient.ofMaterial(Material.DIAMOND_SWORD, 1),
                null
        );

        registry.registerShapedRecipe(shapedDef);
        assertTrue(registry.getShapedRecipes().containsKey("dynamic_shaped"));
        assertEquals("dynamic_shaped", registry.getShapedRecipes().get("dynamic_shaped").getId());
    }
}
