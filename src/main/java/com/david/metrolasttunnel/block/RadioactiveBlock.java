package com.david.metrolasttunnel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RadioactiveBlock extends Block {
    public RadioactiveBlock(Properties properties) { super(properties); }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            player.getPersistentData().putLong("metro_last_radiation_tick", level.getGameTime());
        }
        super.entityInside(state, level, pos, entity);
    }
}
