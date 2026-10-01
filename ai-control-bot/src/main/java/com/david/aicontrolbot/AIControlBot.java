package com.david.aicontrolbot;

import com.david.aicontrolbot.command.BotCommands;
import com.david.aicontrolbot.entity.ModEntities;
import com.david.aicontrolbot.net.LocalControlServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AIControlBot.MOD_ID)
public class AIControlBot {
    public static final String MOD_ID = "aicontrolbot";
    public AIControlBot() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.register(modBus);
        modBus.addListener(AIControlBot::registerAttributes);
        MinecraftForge.EVENT_BUS.addListener(BotCommands::register);
        MinecraftForge.EVENT_BUS.addListener(AIControlBot::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(AIControlBot::serverStopping);
    }
    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CONTROL_BOT.get(), ControlBotEntity.createAttributes().build());
    }
    private static void serverStarted(ServerStartedEvent event) { LocalControlServer.start(event.getServer()); }
    private static void serverStopping(ServerStoppingEvent event) { LocalControlServer.stop(); }
}
