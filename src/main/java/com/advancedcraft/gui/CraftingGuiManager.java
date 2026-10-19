package com.advancedcraft.gui;

import com.advancedcraft.model.CustomIngredient;
import com.advancedcraft.model.ShapedRecipeDefinition;
import com.advancedcraft.model.ShapelessRecipeDefinition;
import com.advancedcraft.service.ItemIdentifierService;
import com.advancedcraft.service.RecipeRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Manages the custom crafting GUI and handles secure inventory interactions.
 */
public class CraftingGuiManager implements Listener {

    private final Plugin plugin;
    private final RecipeRegistry recipeRegistry;
    private final ItemIdentifierService itemIdentifier;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static final String GUI_TITLE = "<gradient:#4158D0:#C850C0>Custom Crafting Table</gradient>";

    public static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int RESULT_SLOT = 24;
    public static final int CRAFT_BUTTON_SLOT = 23;
    public static final int CLEAR_BUTTON_SLOT = 25;

    private final Set<UUID> openGuiSessions = Collections.synchronizedSet(new HashSet<>());

    public CraftingGuiManager(Plugin plugin, RecipeRegistry recipeRegistry, ItemIdentifierService itemIdentifier) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.recipeRegistry = Objects.requireNonNull(recipeRegistry, "RecipeRegistry cannot be null");
        this.itemIdentifier = Objects.requireNonNull(itemIdentifier, "ItemIdentifierService cannot be null");
    }

    public void openCraftingGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, miniMessage.deserialize(GUI_TITLE));
        fillDecorativeGlass(inv);
        setupButtons(inv);
        openGuiSessions.add(player.getUniqueId());
        player.openInventory(inv);
    }

    public void closeAllSessions() {
        for (UUID uuid : new ArrayList<>(openGuiSessions)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.closeInventory();
            }
        }
        openGuiSessions.clear();
    }

    private void fillDecorativeGlass(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }

        Set<Integer> gridSet = new HashSet<>();
        for (int slot : GRID_SLOTS) gridSet.add(slot);

        for (int i = 0; i < 54; i++) {
            if (!gridSet.contains(i) && i != RESULT_SLOT && i != CRAFT_BUTTON_SLOT && i != CLEAR_BUTTON_SLOT) {
                inv.setItem(i, filler);
            }
        }
    }

    private void setupButtons(Inventory inv) {
        ItemStack craftBtn = new ItemStack(Material.NETHER_STAR);
        ItemMeta cMeta = craftBtn.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(miniMessage.deserialize("<green><bold>Craft Item</bold></green>"));
            cMeta.lore(List.of(miniMessage.deserialize("<gray>Click to evaluate recipe and craft.</gray>")));
            craftBtn.setItemMeta(cMeta);
        }
        inv.setItem(CRAFT_BUTTON_SLOT, craftBtn);

        ItemStack clearBtn = new ItemStack(Material.BARRIER);
        ItemMeta clMeta = clearBtn.getItemMeta();
        if (clMeta != null) {
            clMeta.displayName(miniMessage.deserialize("<red><bold>Clear Grid</bold></red>"));
            clMeta.lore(List.of(miniMessage.deserialize("<gray>Returns all ingredients to your inventory.</gray>")));
            clearBtn.setItemMeta(clMeta);
        }
        inv.setItem(CLEAR_BUTTON_SLOT, clearBtn);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!openGuiSessions.contains(player.getUniqueId())) {
            return;
        }

        int rawSlot = event.getRawSlot();
        Inventory topInv = event.getView().getTopInventory();

        if (rawSlot < topInv.getSize()) {
            boolean isGridSlot = isGridSlot(rawSlot);

            if (!isGridSlot) {
                event.setCancelled(true);

                if (rawSlot == CRAFT_BUTTON_SLOT) {
                    evaluateAndCraft(player, topInv);
                } else if (rawSlot == CLEAR_BUTTON_SLOT) {
                    clearGrid(player, topInv);
                } else if (rawSlot == RESULT_SLOT) {
                    claimCraftedResult(player, topInv);
                }
                return;
            }
        }

        if (event.isShiftClick() && rawSlot >= topInv.getSize()) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                moveItemToGrid(topInv, clickedItem);
            }
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY && rawSlot < topInv.getSize()) {
            if (!isGridSlot(rawSlot)) {
                event.setCancelled(true);
            }
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND && rawSlot < topInv.getSize()) {
            if (!isGridSlot(rawSlot)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!openGuiSessions.contains(player.getUniqueId())) {
            return;
        }

        Inventory topInv = event.getView().getTopInventory();
        for (int slot : event.getRawSlots()) {
            if (slot < topInv.getSize() && !isGridSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (openGuiSessions.remove(player.getUniqueId())) {
            Inventory topInv = event.getInventory();
            returnGridItemsToPlayer(player, topInv);
        }
    }

    private boolean isGridSlot(int slot) {
        for (int s : GRID_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    private void moveItemToGrid(Inventory topInv, ItemStack item) {
        for (int slot : GRID_SLOTS) {
            ItemStack current = topInv.getItem(slot);
            if (current == null || current.getType().isAir()) {
                topInv.setItem(slot, item.clone());
                item.setAmount(0);
                return;
            } else if (current.isSimilar(item)) {
                int maxStack = current.getMaxStackSize();
                int space = maxStack - current.getAmount();
                if (space > 0) {
                    int add = Math.min(space, item.getAmount());
                    current.setAmount(current.getAmount() + add);
                    item.setAmount(item.getAmount() - add);
                    if (item.getAmount() <= 0) return;
                }
            }
        }
    }

    private void clearGrid(Player player, Inventory inv) {
        returnGridItemsToPlayer(player, inv);
        inv.setItem(RESULT_SLOT, null);
    }

    private void returnGridItemsToPlayer(Player player, Inventory inv) {
        for (int slot : GRID_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                inv.setItem(slot, null);
            }
        }
    }

    private void evaluateAndCraft(Player player, Inventory inv) {
        ItemStack[] gridItems = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            gridItems[i] = inv.getItem(GRID_SLOTS[i]);
        }

        for (ShapedRecipeDefinition recipe : recipeRegistry.getShapedRecipes().values()) {
            if (matchShapedRecipe(gridItems, recipe)) {
                if (recipe.getPermission() != null && !player.hasPermission(recipe.getPermission())) {
                    player.sendMessage(Component.text("You do not have permission to craft this recipe.", NamedTextColor.RED));
                    return;
                }
                ItemStack result = recipeRegistry.createIngredientItemStack(recipe.getResult());
                inv.setItem(RESULT_SLOT, result);
                player.sendMessage(Component.text("Recipe matched! Click the result to claim.", NamedTextColor.GREEN));
                return;
            }
        }

        for (ShapelessRecipeDefinition recipe : recipeRegistry.getShapelessRecipes().values()) {
            if (matchShapelessRecipe(gridItems, recipe)) {
                if (recipe.getPermission() != null && !player.hasPermission(recipe.getPermission())) {
                    player.sendMessage(Component.text("You do not have permission to craft this recipe.", NamedTextColor.RED));
                    return;
                }
                ItemStack result = recipeRegistry.createIngredientItemStack(recipe.getResult());
                inv.setItem(RESULT_SLOT, result);
                player.sendMessage(Component.text("Recipe matched! Click the result to claim.", NamedTextColor.GREEN));
                return;
            }
        }

        inv.setItem(RESULT_SLOT, null);
        player.sendMessage(Component.text("No matching crafting recipe found.", NamedTextColor.RED));
    }

    private boolean matchShapedRecipe(ItemStack[] gridItems, ShapedRecipeDefinition recipe) {
        List<String> shape = recipe.getShape();
        int shapeRows = shape.size();
        int shapeCols = 0;
        for (String row : shape) {
            shapeCols = Math.max(shapeCols, row.length());
        }

        for (int startRow = 0; startRow <= 3 - shapeRows; startRow++) {
            for (int startCol = 0; startCol <= 3 - shapeCols; startCol++) {
                if (checkShapedOffset(gridItems, recipe, startRow, startCol, shapeRows, shapeCols)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkShapedOffset(ItemStack[] gridItems, ShapedRecipeDefinition recipe,
                                      int startRow, int startCol, int shapeRows, int shapeCols) {
        List<String> shape = recipe.getShape();

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int gridIndex = r * 3 + c;
                ItemStack provided = gridItems[gridIndex];

                boolean inShape = (r >= startRow && r < startRow + shapeRows) &&
                                  (c >= startCol && c < startCol + shapeCols);

                if (inShape) {
                    String rowStr = shape.get(r - startRow);
                    char charAt = (c - startCol < rowStr.length()) ? rowStr.charAt(c - startCol) : ' ';

                    if (charAt == ' ') {
                        if (provided != null && !provided.getType().isAir()) {
                            return false;
                        }
                    } else {
                        CustomIngredient required = recipe.getIngredients().get(charAt);
                        if (!recipeRegistry.matchesIngredient(provided, required)) {
                            return false;
                        }
                    }
                } else {
                    if (provided != null && !provided.getType().isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean matchShapelessRecipe(ItemStack[] gridItems, ShapelessRecipeDefinition recipe) {
        List<ItemStack> providedList = new ArrayList<>();
        for (ItemStack stack : gridItems) {
            if (stack != null && !stack.getType().isAir()) {
                providedList.add(stack.clone());
            }
        }

        List<CustomIngredient> requiredList = recipe.getIngredients();
        if (requiredList.isEmpty()) {
            return false;
        }

        for (CustomIngredient required : requiredList) {
            int neededAmount = required.getAmount();
            int accumulated = 0;

            for (Iterator<ItemStack> it = providedList.iterator(); it.hasNext(); ) {
                ItemStack provided = it.next();
                if (recipeRegistry.matchesIngredient(provided, required)) {
                    int take = Math.min(provided.getAmount(), neededAmount - accumulated);
                    accumulated += take;
                    provided.setAmount(provided.getAmount() - take);
                    if (provided.getAmount() <= 0) {
                        it.remove();
                    }
                    if (accumulated >= neededAmount) {
                        break;
                    }
                }
            }

            if (accumulated < neededAmount) {
                return false;
            }
        }

        return providedList.isEmpty();
    }

    private void claimCraftedResult(Player player, Inventory inv) {
        ItemStack result = inv.getItem(RESULT_SLOT);
        if (result == null || result.getType().isAir()) {
            return;
        }

        ItemStack[] gridItems = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            gridItems[i] = inv.getItem(GRID_SLOTS[i]);
        }

        ShapedRecipeDefinition matchedShaped = null;
        for (ShapedRecipeDefinition recipe : recipeRegistry.getShapedRecipes().values()) {
            if (matchShapedRecipe(gridItems, recipe)) {
                matchedShaped = recipe;
                break;
            }
        }

        if (matchedShaped != null) {
            if (matchedShaped.getPermission() != null && !player.hasPermission(matchedShaped.getPermission())) {
                player.sendMessage(Component.text("You do not have permission to craft this recipe.", NamedTextColor.RED));
                inv.setItem(RESULT_SLOT, null);
                return;
            }

            List<String> shape = matchedShaped.getShape();
            int shapeRows = shape.size();
            int shapeCols = 0;
            for (String row : shape) shapeCols = Math.max(shapeCols, row.length());

            int bestStartRow = -1, bestStartCol = -1;
            for (int r = 0; r <= 3 - shapeRows; r++) {
                for (int c = 0; c <= 3 - shapeCols; c++) {
                    if (checkShapedOffset(gridItems, matchedShaped, r, c, shapeRows, shapeCols)) {
                        bestStartRow = r;
                        bestStartCol = c;
                        break;
                    }
                }
                if (bestStartRow != -1) break;
            }

            if (bestStartRow != -1) {
                for (int r = 0; r < shapeRows; r++) {
                    String rowStr = shape.get(r);
                    for (int c = 0; c < rowStr.length(); c++) {
                        char ch = rowStr.charAt(c);
                        if (ch != ' ') {
                            CustomIngredient required = matchedShaped.getIngredients().get(ch);
                            int gridSlotIndex = GRID_SLOTS[(bestStartRow + r) * 3 + (bestStartCol + c)];
                            ItemStack stack = inv.getItem(gridSlotIndex);
                            if (stack != null && required != null) {
                                stack.setAmount(stack.getAmount() - required.getAmount());
                                if (stack.getAmount() <= 0) {
                                    inv.setItem(gridSlotIndex, null);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            ShapelessRecipeDefinition matchedShapeless = null;
            for (ShapelessRecipeDefinition recipe : recipeRegistry.getShapelessRecipes().values()) {
                if (matchShapelessRecipe(gridItems, recipe)) {
                    matchedShapeless = recipe;
                    break;
                }
            }

            if (matchedShapeless != null) {
                if (matchedShapeless.getPermission() != null && !player.hasPermission(matchedShapeless.getPermission())) {
                    player.sendMessage(Component.text("You do not have permission to craft this recipe.", NamedTextColor.RED));
                    inv.setItem(RESULT_SLOT, null);
                    return;
                }

                for (CustomIngredient required : matchedShapeless.getIngredients()) {
                    int needed = required.getAmount();
                    for (int slot : GRID_SLOTS) {
                        ItemStack stack = inv.getItem(slot);
                        if (stack != null && recipeRegistry.matchesIngredient(stack, required)) {
                            int take = Math.min(stack.getAmount(), needed);
                            needed -= take;
                            stack.setAmount(stack.getAmount() - take);
                            if (stack.getAmount() <= 0) {
                                inv.setItem(slot, null);
                            }
                            if (needed <= 0) break;
                        }
                    }
                }
            } else {
                inv.setItem(RESULT_SLOT, null);
                return;
            }
        }

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result.clone());
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        inv.setItem(RESULT_SLOT, null);
        player.sendMessage(Component.text("Successfully crafted item!", NamedTextColor.GREEN));

        evaluateAndCraft(player, inv);
    }
}
