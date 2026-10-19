package com.advancedcraft.model;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

/**
 * Data model for registered custom items.
 */
public class CustomItemDefinition {

    private final String id;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final Integer customModelData;
    private final Map<Enchantment, Integer> enchantments;
    private final Set<ItemFlag> itemFlags;
    private final boolean unbreakable;
    private final Integer maxStackSize;
    private final Integer durability;
    private final Map<Attribute, List<AttributeModifier>> attributeModifiers;
    private final Map<String, String> persistentMetadata;
    private final String obtainPermission;
    private final String usePermission;

    public CustomItemDefinition(String id,
                                Material material,
                                String name,
                                List<String> lore,
                                Integer customModelData,
                                Map<Enchantment, Integer> enchantments,
                                Set<ItemFlag> itemFlags,
                                boolean unbreakable,
                                Integer maxStackSize,
                                Integer durability,
                                Map<Attribute, List<AttributeModifier>> attributeModifiers,
                                Map<String, String> persistentMetadata,
                                String obtainPermission,
                                String usePermission) {
        this.id = Objects.requireNonNull(id, "Item ID cannot be null").toLowerCase(Locale.ROOT);
        this.material = Objects.requireNonNull(material, "Material cannot be null");
        this.name = name;
        this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
        this.customModelData = customModelData;
        this.enchantments = enchantments != null ? new HashMap<>(enchantments) : new HashMap<>();
        this.itemFlags = (itemFlags != null && !itemFlags.isEmpty()) ? EnumSet.copyOf(itemFlags) : EnumSet.noneOf(ItemFlag.class);
        this.unbreakable = unbreakable;
        this.maxStackSize = maxStackSize;
        this.durability = durability;
        this.attributeModifiers = attributeModifiers != null ? new HashMap<>(attributeModifiers) : new HashMap<>();
        this.persistentMetadata = persistentMetadata != null ? new HashMap<>(persistentMetadata) : new HashMap<>();
        this.obtainPermission = obtainPermission;
        this.usePermission = usePermission;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return Collections.unmodifiableMap(enchantments);
    }

    public Set<ItemFlag> getItemFlags() {
        return Collections.unmodifiableSet(itemFlags);
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public Integer getMaxStackSize() {
        return maxStackSize;
    }

    public Integer getDurability() {
        return durability;
    }

    public Map<Attribute, List<AttributeModifier>> getAttributeModifiers() {
        return Collections.unmodifiableMap(attributeModifiers);
    }

    public Map<String, String> getPersistentMetadata() {
        return Collections.unmodifiableMap(persistentMetadata);
    }

    public String getObtainPermission() {
        return obtainPermission;
    }

    public String getUsePermission() {
        return usePermission;
    }
}
