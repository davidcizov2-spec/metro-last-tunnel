package com.david.metrolasttunnel.item;

import com.david.metrolasttunnel.event.RadiationSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DosimeterItem extends Item {
    public DosimeterItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            int radiation = RadiationSystem.getRadiation(player);
            String state = radiation < 20 ? "чисто" : radiation < 50 ? "повышено" : radiation < 100 ? "опасно" : "критически опасно";
            player.displayClientMessage(Component.literal("Дозиметр: " + radiation + " — " + state), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
