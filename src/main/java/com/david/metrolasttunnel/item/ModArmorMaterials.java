package com.david.metrolasttunnel.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public enum ModArmorMaterials implements ArmorMaterial {
    FABRIC("gas_mask", 7, new int[]{1, 2, 2, 1}, 10, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, Ingredient.of(Items.LEATHER));

    private static final int[] HEALTH_PER_TYPE = {13, 15, 16, 11};
    private final String name;
    private final int durability;
    private final int[] defense;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Ingredient repair;

    ModArmorMaterials(String name, int durability, int[] defense, int enchantmentValue, SoundEvent equipSound,
                      float toughness, float knockbackResistance, Ingredient repair) {
        this.name = name;
        this.durability = durability;
        this.defense = defense;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repair = repair;
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return HEALTH_PER_TYPE[type.getSlot().getIndex()] * durability; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return defense[type.getSlot().getIndex()]; }
    @Override public int getEnchantmentValue() { return enchantmentValue; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() { return repair; }
    @Override public String getName() { return "metrolasttunnel:" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
