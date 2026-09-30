package com.david.metrolasttunnel.item;

import com.david.metrolasttunnel.event.PlayerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FlashlightItem extends Item {
    public FlashlightItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            boolean active = PlayerEvents.toggleFlashlight(player);
            player.displayClientMessage(Component.literal(active ? "Фонарик включён" : "Фонарик выключен"), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
