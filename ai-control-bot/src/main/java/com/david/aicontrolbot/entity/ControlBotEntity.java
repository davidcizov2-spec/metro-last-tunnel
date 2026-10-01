package com.david.aicontrolbot.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

public class ControlBotEntity extends PathfinderMob {
    private enum BrainState {
        GATHER_WOOD,
        BUILD_HOME,
        MINE,
        RETURN_HOME
    }

    private int inputTicks;
    private float forward;
    private float strafe;

    private BrainState state = BrainState.GATHER_WOOD;
    private BlockPos homeOrigin;
    private int wood = 0;
    private int stone = 0;
    private int ores = 0;
    private int buildIndex = 0;
    private int mineProgress = 0;
    private int idleTicks = 0;
    private int statusTicks = 0;
    private String lastStatus = "";

    protected ControlBotEntity(net.minecraft.world.entity.EntityType<? extends PathfinderMob> type,
                               net.minecraft.world.level.Level level) {
        super(type, level);
        this.homeOrigin = blockPosition().offset(3, 0, 3);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
    }

    public void setInput(float forward, float strafe, int ticks) {
        this.forward = forward;
        this.strafe = strafe;
        inputTicks = Math.max(inputTicks, ticks);
        getNavigation().stop();
    }

    public void stopInput() {
        forward = 0;
        strafe = 0;
        inputTicks = 0;
        getNavigation().stop();
    }

    public void jumpNow() {
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().x, 0.42D, getDeltaMovement().z);
            hasImpulse = true;
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (inputTicks > 0) {
            processManualInput();
        }

        if (statusTicks > 0) {
            statusTicks--;
        }

        if (tickCount % 10 == 0) {
            makeZombiesReact();
            recoverHealth();
        }

        if (getTarget() != null && getTarget().isAlive()) {
            setStatus("Сражаюсь с зомби");
            return;
        }

        if (tickCount % 20 == 0) {
            runSurvivalBrain();
        }
    }

    private void processManualInput() {
        inputTicks--;

        float yaw = (float) Math.toRadians(getYRot());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = Math.cos(yaw);
        double rz = Math.sin(yaw);

        double x = fx * forward + rx * strafe;
        double z = fz * forward + rz * strafe;
        double len = Math.sqrt(x * x + z * z);

        if (len > 0.001D) {
            move(net.minecraft.world.entity.MoverType.SELF,
                    new net.minecraft.world.phys.Vec3(x / len * 0.09D, 0, z / len * 0.09D));
        }
    }

    private void makeZombiesReact() {
        for (Zombie zombie : level().getEntitiesOfClass(
                Zombie.class,
                getBoundingBox().inflate(14.0D))) {

            if (!zombie.isAlive()) {
                continue;
            }

            double distance = distanceToSqr(zombie);

            if (distance < 16.0D || zombie.getTarget() == null) {
                zombie.setTarget(this);
            }

            if (getTarget() == null && distance < 196.0D) {
                setTarget(zombie);
            }
        }
    }

    private void recoverHealth() {
        if (getHealth() < getMaxHealth() * 0.45F) {
            setHealth(Math.min(getMaxHealth(), getHealth() + 1.0F));
            setStatus("Восстанавливаю здоровье");
        }
    }

    private void runSurvivalBrain() {
        if (homeOrigin == null) {
            homeOrigin = blockPosition().offset(3, 0, 3);
        }

        switch (state) {
            case GATHER_WOOD -> gatherWood();
            case BUILD_HOME -> buildHome();
            case MINE -> mine();
            case RETURN_HOME -> returnHome();
        }
    }

    private void gatherWood() {
        if (wood >= 12) {
            state = BrainState.BUILD_HOME;
            buildIndex = 0;
            getNavigation().stop();
            setStatus("Строю дом");
            return;
        }

        BlockPos log = findNearestLog(14);

        if (log == null) {
            idleTicks++;
            setStatus("Ищу дерево");

            if (idleTicks > 8) {
                idleTicks = 0;
                wander();
            }
            return;
        }

        idleTicks = 0;
        setStatus("Добываю дерево");
        moveNear(log);

        if (distanceToSqr(log.getX() + 0.5D, log.getY() + 0.5D, log.getZ() + 0.5D) < 7.0D) {
            BlockState blockState = level().getBlockState(log);
            if (blockState.is(BlockTags.LOGS)) {
                Block block = blockState.getBlock();
                if (level().destroyBlock(log, false, this)) {
                    wood++;
                    block.popResource(level(), log, block.asItem().getDefaultInstance());
                }
            }
        }
    }

    private void buildHome() {
        setStatus("Строю дом");

        final int width = 7;
        final int depth = 7;
        final int height = 5;

        int total = width * depth * height;
        if (buildIndex < total) {
            int index = buildIndex++;
            int x = index % width;
            int z = (index / width) % depth;
            int y = index / (width * depth);

            boolean boundary = x == 0 || x == width - 1 || z == 0 || z == depth - 1 || y == height - 1;
            boolean doorOpening = z == 0 && x == 3 && y < 2;

            if (boundary && !doorOpening) {
                BlockPos p = homeOrigin.offset(x, y, z);

                if (level().isEmptyBlock(p)) {
                    level().setBlock(p, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    wood = Math.max(0, wood - 1);
                } else if (level().getBlockState(p).is(BlockTags.LOGS)) {
                    level().destroyBlock(p, false, this);
                    level().setBlock(p, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                }
            }

            if (wood <= 0 && buildIndex < total) {
                state = BrainState.GATHER_WOOD;
                setStatus("Нужно ещё дерево");
            }
            return;
        }

        BlockPos table = homeOrigin.offset(3, 0, 3);
        if (level().isEmptyBlock(table)) {
            level().setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        }

        state = BrainState.MINE;
        mineProgress = 0;
        getNavigation().stop();
        setStatus("Иду в шахту");
    }

    private void mine() {
        if (mineProgress >= 24) {
            state = BrainState.RETURN_HOME;
            setStatus("Возвращаюсь домой");
            return;
        }

        BlockPos target = homeOrigin.offset(3 + mineProgress, 0, 3);

        if (mineProgress % 5 == 0) {
            BlockPos ore = findNearestOre(7);
            if (ore != null) {
                setStatus("Добываю руду");
                moveNear(ore);

                if (distanceToSqr(ore.getX() + 0.5D, ore.getY() + 0.5D, ore.getZ() + 0.5D) < 8.0D) {
                    if (level().destroyBlock(ore, false, this)) {
                        ores++;
                    }
                }
                return;
            }
        }

        setStatus("Копаю шахту");
        moveNear(target);

        if (distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) < 10.0D) {
            BlockState feet = level().getBlockState(target);
            BlockState head = level().getBlockState(target.above());

            if (!feet.isAir()) {
                if (level().destroyBlock(target, false, this)) {
                    stone++;
                }
            }

            if (!head.isAir() && !head.is(BlockTags.LOGS)) {
                level().destroyBlock(target.above(), false, this);
            }

            mineProgress++;
        }
    }

    private void returnHome() {
        setStatus("Возвращаюсь домой");
        BlockPos center = homeOrigin.offset(3, 0, 3);

        moveNear(center);

        if (distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) < 12.0D) {
            state = BrainState.GATHER_WOOD;
            setStatus("Снова ищу дерево");
        }
    }

    private BlockPos findNearestLog(int radius) {
        BlockPos center = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -4; y <= 7; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (!level().hasChunkAt(p)) {
                        continue;
                    }

                    if (level().getBlockState(p).is(BlockTags.LOGS)) {
                        double d = p.distSqr(center);
                        if (d < bestDistance) {
                            bestDistance = d;
                            best = p;
                        }
                    }
                }
            }
        }

        return best;
    }

    private BlockPos findNearestOre(int radius) {
        BlockPos center = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.offset(x, y, z);
                    Block block = level().getBlockState(p).getBlock();

                    if (block == Blocks.COAL_ORE || block == Blocks.IRON_ORE
                            || block == Blocks.COPPER_ORE || block == Blocks.GOLD_ORE
                            || block == Blocks.REDSTONE_ORE || block == Blocks.LAPIS_ORE
                            || block == Blocks.DIAMOND_ORE || block == Blocks.EMERALD_ORE) {
                        double d = p.distSqr(center);
                        if (d < bestDistance) {
                            bestDistance = d;
                            best = p;
                        }
                    }
                }
            }
        }

        return best;
    }

    private void moveNear(BlockPos pos) {
        getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.0D);
        getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private void wander() {
        double angle = getRandom().nextDouble() * Math.PI * 2.0D;
        int distance = 5 + getRandom().nextInt(8);
        double x = getX() + Math.cos(angle) * distance;
        double z = getZ() + Math.sin(angle) * distance;

        getNavigation().moveTo(x, getY(), z, 0.8D);
    }

    private void setStatus(String status) {
        if (status.equals(lastStatus) && statusTicks > 0) {
            return;
        }

        lastStatus = status;
        statusTicks = 40;
        setCustomName(Component.literal("ChatBot • " + status));
        setCustomNameVisible(true);
    }

    public int getWood() {
        return wood;
    }

    public int getStone() {
        return stone;
    }

    public int getOres() {
        return ores;
    }

    public void faceNearestPlayer() {
        LivingEntity p = level().getNearestPlayer(this, 32.0D);
        if (p != null) {
            getLookControl().setLookAt(p);
        }
    }
}
