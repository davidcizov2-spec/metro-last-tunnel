package com.david.detectiveid.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public class DetectiveIdClientEvents {
    private static final ResourceLocation ID_TEXTURE =
            new ResourceLocation("detective_id", "textures/items/detective_id.png");

    private final ModelRenderer arm;
    private final ModelRenderer armOverlay;
    private final ModelRenderer leftPage;
    private final ModelRenderer rightPage;
    private final ModelBase skinModel;
    private final ModelBase idModel;

    private long animationStart = -1L;
    private float progress = 0.0F;

    public DetectiveIdClientEvents() {
        skinModel = new ModelBase() {};
        idModel = new ModelBase() {};

        arm = new ModelRenderer(skinModel, 40, 16);
        arm.setTextureSize(64, 64);
        arm.addBox(-2.0F, -12.0F, -2.0F, 4, 12, 4);

        armOverlay = new ModelRenderer(skinModel, 40, 32);
        armOverlay.setTextureSize(64, 64);
        armOverlay.addBox(-2.25F, -12.25F, -2.25F, 4, 12, 4);

        leftPage = new ModelRenderer(idModel, 0, 6);
        leftPage.setTextureSize(16, 16);
        leftPage.addBox(-1.70F, -1.10F, -0.08F, 2, 3, 3);

        rightPage = new ModelRenderer(idModel, 0, 0);
        rightPage.setTextureSize(16, 16);
        rightPage.addBox(-0.30F, -1.10F, -0.08F, 2, 3, 3);
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (isDetectiveDye(event.getItemStack()) && !event.getToolTip().isEmpty()) {
            event.getToolTip().set(0, "§fУдостоверение детектива");
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != EnumHand.MAIN_HAND) return;
        if (event.getEntityPlayer() != Minecraft.getMinecraft().player) return;
        if (!isDetectiveDye(event.getItemStack())) return;

        animationStart = System.nanoTime();
        progress = 0.0F;
    }

    @SubscribeEvent
    public void onRenderHand(RenderSpecificHandEvent event) {
        if (event.getHand() != EnumHand.MAIN_HAND) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        ItemStack held = player.getHeldItemMainhand();
        if (!isDetectiveDye(held)) return;
        if (animationStart < 0L) return;

        double elapsed = (System.nanoTime() - animationStart) / 1000000000.0D;
        progress = MathHelper.clamp((float)(elapsed / 0.625D), 0.0F, 1.0F);
        float openProgress = MathHelper.clamp((float)(elapsed / 0.125D), 0.0F, 1.0F);

        event.setCanceled(true);
        renderFirstPerson(mc, player, openProgress, progress);
    }

    private boolean isDetectiveDye(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ItemDye)) return false;
        int meta = stack.getMetadata();
        return meta == 12 || meta == 6;
    }

    private void renderFirstPerson(Minecraft mc, EntityPlayer player,
                                   float openProgress, float totalProgress) {
        TextureManager tm = mc.getTextureManager();

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        float raise = smoothstep(MathHelper.clamp(totalProgress / 0.72F, 0.0F, 1.0F));

        GlStateManager.translate(
                0.52D - 0.24D * raise,
                0.92D - 1.18D * raise,
                -0.55D - 0.10D * raise);

        GlStateManager.rotate(-16.0F + 16.0F * raise, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-24.0F + 18.0F * raise, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(28.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.95D, 0.95D, 0.95D);

        tm.bindTexture(player.getLocationSkin());
        arm.render(0.0625F);
        armOverlay.render(0.0625F);

        GlStateManager.translate(-0.18D, -0.20D, -0.24D);
        GlStateManager.rotate(74.0F - 12.0F * raise, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(4.0F + 8.0F * raise, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.16D, 0.16D, 0.16D);

        tm.bindTexture(ID_TEXTURE);

        GlStateManager.pushMatrix();
        leftPage.render(0.0625F);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.rotate(
                -135.0F * smoothstep(openProgress),
                0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.14D, 0.0D, 0.34D);
        rightPage.render(0.0625F);
        GlStateManager.popMatrix();

        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    private static float smoothstep(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
