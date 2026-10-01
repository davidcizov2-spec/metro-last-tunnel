package com.david.aicontrolbot.client;

import com.david.aicontrolbot.AIControlBot;
import com.david.aicontrolbot.entity.ControlBotEntity;
import com.david.aicontrolbot.entity.ModEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AIControlBot.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class AIControlBotClient {
    private AIControlBotClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CONTROL_BOT.get(), BotRenderer::new);
    }

    private static final class BotRenderer
            extends MobRenderer<ControlBotEntity, HumanoidModel<ControlBotEntity>> {

        private static final ResourceLocation TEXTURE =
                new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

        private BotRenderer(EntityRendererProvider.Context context) {
            super(context,
                    new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
                    0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(ControlBotEntity entity) {
            return TEXTURE;
        }
    }
}
