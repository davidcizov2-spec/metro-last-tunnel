package com.david.metrolasttunnel.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MedkitItem extends Item {
    public MedkitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player.getHealth() < player.getMaxHealth() && !player.getCooldowns().isOnCooldown(this)) {
            player.heal(6.0F);
            player.getCooldowns().addCooldown(this, 40);
            stack.shrink(1);
            player.displayClientMessage(Component.literal("Аптечка использована: +3 сердца"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
