package com.david.metrolasttunnel.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

public class GasMaskItem extends ArmorItem {
    public GasMaskItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public static boolean isWorn(net.minecraft.world.entity.player.Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GasMaskItem;
    }
}
