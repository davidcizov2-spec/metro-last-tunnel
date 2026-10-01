package com.david.aicontrolbot.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class ControlBotEntity extends PathfinderMob {
    private int inputTicks;
    private float forward;
    private float strafe;

    protected ControlBotEntity(net.minecraft.world.entity.EntityType<? extends PathfinderMob> type,
                               net.minecraft.world.level.Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D).add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public void setInput(float forward, float strafe, int ticks) {
        this.forward = forward; this.strafe = strafe; inputTicks = Math.max(inputTicks, ticks);
        getNavigation().stop();
    }
    public void stopInput() { forward = 0; strafe = 0; inputTicks = 0; getNavigation().stop(); }
    public void jumpNow() {
        if (onGround()) { setDeltaMovement(getDeltaMovement().x, 0.42D, getDeltaMovement().z); hasImpulse = true; }
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && inputTicks > 0) {
            inputTicks--;
            float yaw = (float)Math.toRadians(getYRot());
            double fx = -Math.sin(yaw), fz = Math.cos(yaw);
            double rx = Math.cos(yaw), rz = Math.sin(yaw);
            double x = fx * forward + rx * strafe, z = fz * forward + rz * strafe;
            double len = Math.sqrt(x*x + z*z);
            if (len > 0.001D) move(net.minecraft.world.entity.MoverType.SELF, new Vec3(x/len*0.09D, 0, z/len*0.09D));
        }
    }
    public void faceNearestPlayer() {
        LivingEntity p = level().getNearestPlayer(this, 32.0D);
        if (p != null) getLookControl().setLookAt(p);
    }
}
