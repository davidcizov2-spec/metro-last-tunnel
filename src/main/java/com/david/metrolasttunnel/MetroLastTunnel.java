package com.david.metrolasttunnel;

import com.david.metrolasttunnel.block.ModBlocks;
import com.david.metrolasttunnel.event.PlayerEvents;
import com.david.metrolasttunnel.item.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MetroLastTunnel.MODID)
public class MetroLastTunnel {
    public static final String MODID = "metrolasttunnel";

    public MetroLastTunnel() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modBus);
        ModBlocks.register(modBus);
        MinecraftForge.EVENT_BUS.register(PlayerEvents.class);
    }
}
