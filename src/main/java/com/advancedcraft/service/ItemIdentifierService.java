package com.advancedcraft.service;

import com.advancedcraft.model.CustomItemDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Handles persistent item identification and ItemStack creation/validation for custom items.
 */
public class ItemIdentifierService {

    public static final String ITEM_ID_KEY_NAME = "custom_item_id";
    private final NamespacedKey customItemKey;
    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ItemIdentifierService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.customItemKey = new NamespacedKey(plugin, ITEM_ID_KEY_NAME);
    }

    public NamespacedKey getCustomItemKey() {
        return customItemKey;
    }

    /**
     * Retrieves the custom item ID from an ItemStack if present in PDC.
     */
    public Optional<String> getCustomItemId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(customItemKey, PersistentDataType.STRING)) {
            String id = pdc.get(customItemKey, PersistentDataType.STRING);
            return Optional.ofNullable(id);
        }
        return Optional.empty();
    }

    /**
     * Checks whether an ItemStack matches a specific registered custom item ID.
     */
    public boolean isCustomItem(ItemStack itemStack, String expectedId) {
        if (expectedId == null) {
            return false;
        }
        return getCustomItemId(itemStack)
                .map(id -> id.equalsIgnoreCase(expectedId))
                .orElse(false);
    }

    /**
     * Creates a new ItemStack from a CustomItemDefinition with specified count.
     */
    public ItemStack createItemStack(CustomItemDefinition definition, int amount) {
        Objects.requireNonNull(definition, "Definition cannot be null");
        ItemStack stack = new ItemStack(definition.getMaterial(), Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            // Apply persistent custom item ID
            meta.getPersistentDataContainer().set(customItemKey, PersistentDataType.STRING, definition.getId());

            // Apply display name
            if (definition.getName() != null && !definition.getName().isEmpty()) {
                meta.displayName(miniMessage.deserialize(definition.getName()));
            }

            // Apply lore
            if (!definition.getLore().isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : definition.getLore()) {
                    loreComponents.add(miniMessage.deserialize(line));
                }
                meta.lore(loreComponents);
            }

            // Apply custom model data if present
            if (definition.getCustomModelData() != null) {
                meta.setCustomModelData(definition.getCustomModelData());
            }

            // Apply enchantments
            if (!definition.getEnchantments().isEmpty()) {
                for (Map.Entry<Enchantment, Integer> entry : definition.getEnchantments().entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            // Apply item flags
            if (!definition.getItemFlags().isEmpty()) {
                meta.addItemFlags(definition.getItemFlags().toArray(new ItemFlag[0]));
            }

            // Apply unbreakable state
            meta.setUnbreakable(definition.isUnbreakable());

            // Apply attribute modifiers
            if (!definition.getAttributeModifiers().isEmpty()) {
                for (Map.Entry<Attribute, List<AttributeModifier>> entry : definition.getAttributeModifiers().entrySet()) {
                    for (AttributeModifier modifier : entry.getValue()) {
                        meta.addAttributeModifier(entry.getKey(), modifier);
                    }
                }
            }

            // Apply custom persistent metadata entries
            if (!definition.getPersistentMetadata().isEmpty()) {
                for (Map.Entry<String, String> entry : definition.getPersistentMetadata().entrySet()) {
                    NamespacedKey metaKey = new NamespacedKey(plugin, entry.getKey().toLowerCase(Locale.ROOT));
                    meta.getPersistentDataContainer().set(metaKey, PersistentDataType.STRING, entry.getValue());
                }
            }

            // Apply max stack size if API supported
            if (definition.getMaxStackSize() != null) {
                try {
                    meta.setMaxStackSize(definition.getMaxStackSize());
                } catch (Throwable ignored) {
                    // Fallback for versions where setMaxStackSize on meta is restricted
                }
            }

            // Apply durability / damage if Damageable
            if (definition.getDurability() != null && meta instanceof Damageable damageable) {
                damageable.setDamage(definition.getDurability());
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }
}
