package com.david.detectiveid;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = DetectiveIdMod.MODID, name = DetectiveIdMod.NAME, version = DetectiveIdMod.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.12.2]")
public class DetectiveIdMod {
    public static final String MODID = "detective_id";
    public static final String NAME = "Detective ID Animation";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new com.david.detectiveid.client.DetectiveIdClientEvents());
        }
    }
}
