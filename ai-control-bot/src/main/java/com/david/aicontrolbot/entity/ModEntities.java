package com.david.aicontrolbot.entity;

import com.david.aicontrolbot.AIControlBot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AIControlBot.MOD_ID);
    public static final RegistryObject<EntityType<ControlBotEntity>> CONTROL_BOT =
            ENTITY_TYPES.register("control_bot", () -> EntityType.Builder.of(ControlBotEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(8).updateInterval(2).build("control_bot"));
    private ModEntities() {}
    public static void register(IEventBus bus) { ENTITY_TYPES.register(bus); }
}
