package com.david.aicontrolbot.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.phys.Vec3;

public class ControlBotEntity extends PathfinderMob {
    private enum BrainState {
        GATHER_WOOD,
        BUILD_HOME,
        MINE,
        RETURN_HOME
    }

    private final SimpleContainer botInventory = new SimpleContainer(36);

    private int inputTicks;
    private float forward;
    private float strafe;

    private BrainState state = BrainState.GATHER_WOOD;
    private BlockPos homeOrigin;
    private int buildIndex;
    private int mineProgress;
    private int idleTicks;
    private int statusTicks;
    private String lastStatus = "";

    private BlockPos breakingTarget;
    private int breakingTicks;
    private int breakingRequired;

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

    public SimpleContainer getBotInventory() {
        return botInventory;
    }

    public void openInventory(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> ChestMenu.sixRows(id, inventory, botInventory),
                Component.literal("ChatBot — инвентарь")));
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

        if (tickCount % 5 == 0) {
            collectNearbyItems();
            equipArmorFromInventory();
        }

        if (tickCount % 10 == 0) {
            makeZombiesReact();
            recoverHealth();
        }

        if (breakingTarget != null) {
            tickBreaking();
        }

        if (getTarget() != null && getTarget().isAlive()) {
            cancelBreaking();
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
                    new Vec3(x / len * 0.09D, 0, z / len * 0.09D));
        }
    }

    private void collectNearbyItems() {
        for (ItemEntity itemEntity : level().getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(3.0D))) {

            if (!itemEntity.isAlive() || itemEntity.getItem().isEmpty()) {
                continue;
            }

            ItemStack remaining = botInventory.addItem(itemEntity.getItem().copy());
            itemEntity.setItem(remaining);

            if (remaining.isEmpty()) {
                itemEntity.discard();
            }

            if (!remaining.equals(itemEntity.getItem())) {
                setStatus("Подбираю предметы");
            }
        }
    }

    private void equipArmorFromInventory() {
        for (int i = 0; i < botInventory.getContainerSize(); i++) {
            ItemStack stack = botInventory.getItem(i);

            if (!(stack.getItem() instanceof ArmorItem armor)) {
                continue;
            }

            EquipmentSlot slot = armor.getEquipmentSlot();
            ItemStack equipped = getItemBySlot(slot);

            if (equipped.isEmpty()) {
                botInventory.setItem(i, ItemStack.EMPTY);
                setItemSlot(slot, stack.split(1));
                setStatus("Надеваю броню");
            }
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

            if (distance < 196.0D) {
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

        prepareWoodMaterials();

        switch (state) {
            case GATHER_WOOD -> gatherWood();
            case BUILD_HOME -> buildHome();
            case MINE -> mine();
            case RETURN_HOME -> returnHome();
        }
    }

    private void gatherWood() {
        int logs = countItem(Items.OAK_LOG) + countItem(Items.BIRCH_LOG)
                + countItem(Items.SPRUCE_LOG) + countItem(Items.JUNGLE_LOG);

        if (countPlanks() >= 24 || logs >= 8) {
            state = BrainState.BUILD_HOME;
            buildIndex = 0;
            getNavigation().stop();
            setStatus("Строю дом");
            return;
        }

        BlockPos log = findNearestLog(16);

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

        if (canReachBlock(log)) {
            startOrContinueBreaking(log);
        }
    }

    private void buildHome() {
        setStatus("Строю дом");

        final int width = 7;
        final int depth = 7;
        final int height = 5;
        final int total = width * depth * height;

        while (buildIndex < total) {
            int index = buildIndex++;
            int x = index % width;
            int z = (index / width) % depth;
            int y = index / (width * depth);

            boolean boundary = x == 0 || x == width - 1 || z == 0 || z == depth - 1 || y == height - 1;
            boolean doorOpening = z == 0 && x == 3 && y < 2;

            if (!boundary || doorOpening) {
                continue;
            }

            BlockPos p = homeOrigin.offset(x, y, z);

            if (level().isEmptyBlock(p)) {
                if (!takeItem(Items.OAK_PLANKS, 1)) {
                    state = BrainState.GATHER_WOOD;
                    setStatus("Не хватает досок");
                    buildIndex = Math.max(0, buildIndex - 1);
                    return;
                }

                level().setBlock(p, Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }

            return;
        }

        BlockPos table = homeOrigin.offset(3, 0, 3);
        if (level().isEmptyBlock(table)) {
            level().setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        }

        state = BrainState.MINE;
        mineProgress = 0;
        breakingTarget = null;
        setStatus("Иду в шахту");
    }

    private void mine() {
        if (mineProgress >= 24) {
            state = BrainState.RETURN_HOME;
            setStatus("Возвращаюсь домой");
            return;
        }

        BlockPos target = homeOrigin.offset(3 + mineProgress, 0, 3);

        BlockPos ore = findNearestOre(8);
        if (ore != null && canReachBlock(ore)) {
            setStatus("Добываю руду");
            startOrContinueBreaking(ore);
            return;
        }

        setStatus("Копаю шахту");
        moveNear(target);

        if (canReachBlock(target)) {
            startOrContinueBreaking(target);
        }

        if (level().getBlockState(target).isAir() && level().getBlockState(target.above()).isAir()) {
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

    private void prepareWoodMaterials() {
        int logs = countItem(Items.OAK_LOG) + countItem(Items.BIRCH_LOG)
                + countItem(Items.SPRUCE_LOG) + countItem(Items.JUNGLE_LOG);

        if (logs > 0 && countPlanks() < 32) {
            int converted = Math.min(logs, 8);

            for (int i = 0; i < botInventory.getContainerSize() && converted > 0; i++) {
                ItemStack stack = botInventory.getItem(i);
                Item item = stack.getItem();

                if (item == Items.OAK_LOG || item == Items.BIRCH_LOG
                        || item == Items.SPRUCE_LOG || item == Items.JUNGLE_LOG) {
                    int amount = Math.min(converted, stack.getCount());
                    stack.shrink(amount);
                    botInventory.setItem(i, stack);
                    botInventory.addItem(new ItemStack(Items.OAK_PLANKS, amount * 4));
                    converted -= amount;
                }
            }
        }

        if (countPlanks() >= 8 && !hasItem(Items.WOODEN_PICKAXE)) {
            if (takeItem(Items.OAK_PLANKS, 5)) {
                botInventory.addItem(new ItemStack(Items.WOODEN_PICKAXE));
            }
        }

        if (countPlanks() >= 5 && !hasItem(Items.WOODEN_AXE)) {
            if (takeItem(Items.OAK_PLANKS, 3)) {
                botInventory.addItem(new ItemStack(Items.WOODEN_AXE));
            }
        }
    }

    private BlockPos findNearestLog(int radius) {
        BlockPos center = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -4; y <= 10; y++) {
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
            for (int y = -8; y <= 6; y++) {
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

    private boolean canReachBlock(BlockPos pos) {
        double dx = pos.getX() + 0.5D - getX();
        double dy = pos.getY() + 0.5D - (getY() + getBbHeight() * 0.65D);
        double dz = pos.getZ() + 0.5D - getZ();
        return dx * dx + dy * dy + dz * dz <= 4.7D * 4.7D;
    }

    private void startOrContinueBreaking(BlockPos pos) {
        BlockState state = level().getBlockState(pos);

        if (state.isAir() || state.getDestroySpeed(level(), pos) < 0.0F) {
            cancelBreaking();
            return;
        }

        if (breakingTarget == null || !breakingTarget.equals(pos)) {
            breakingTarget = pos.immutable();
            breakingTicks = 0;
            breakingRequired = calculateBreakTime(state);
        }

        breakingTicks++;

        if (breakingTicks >= breakingRequired) {
            boolean destroyed = level().destroyBlock(pos, true, this);

            if (destroyed) {
                mineProgress++;
                prepareWoodMaterials();
                collectNearbyItems();
                equipArmorFromInventory();
            }

            cancelBreaking();
        } else {
            int seconds = Math.max(1, (breakingRequired - breakingTicks + 19) / 20);
            setStatus("Ломаю блок • " + seconds + "с");
        }
    }

    private int calculateBreakTime(BlockState state) {
        float hardness = state.getDestroySpeed(level(), breakingTarget == null ? blockPosition() : breakingTarget);

        if (hardness < 0.0F) {
            return Integer.MAX_VALUE;
        }

        float speed = 1.0F;

        if (state.is(BlockTags.LOGS) && hasItem(Items.WOODEN_AXE)) {
            speed = 2.0F;
        }

        if (!state.is(BlockTags.LOGS) && state.is(BlockTags.MINEABLE_PICKAXE) && hasAnyPickaxe()) {
            speed = 2.0F;
        }

        return Math.max(5, Math.round(hardness * 20.0F / speed));
    }

    private void cancelBreaking() {
        breakingTarget = null;
        breakingTicks = 0;
        breakingRequired = 0;
    }

    private boolean hasAnyPickaxe() {
        return hasItem(Items.WOODEN_PICKAXE) || hasItem(Items.STONE_PICKAXE)
                || hasItem(Items.IRON_PICKAXE) || hasItem(Items.DIAMOND_PICKAXE);
    }

    private int countPlanks() {
        return countItem(Items.OAK_PLANKS);
    }

    private int countItem(Item item) {
        int count = 0;

        for (int i = 0; i < botInventory.getContainerSize(); i++) {
            if (botInventory.getItem(i).is(item)) {
                count += botInventory.getItem(i).getCount();
            }
        }

        return count;
    }

    private boolean hasItem(Item item) {
        return countItem(item) > 0;
    }

    private boolean takeItem(Item item, int amount) {
        int remaining = amount;

        for (int i = 0; i < botInventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = botInventory.getItem(i);

            if (!stack.is(item)) {
                continue;
            }

            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;

            if (stack.isEmpty()) {
                botInventory.setItem(i, ItemStack.EMPTY);
            }
        }

        return remaining == 0;
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

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BotState", state.ordinal());
        tag.putInt("MineProgress", mineProgress);
        tag.putInt("BuildIndex", buildIndex);

        CompoundTag home = new CompoundTag();
        home.putInt("X", homeOrigin.getX());
        home.putInt("Y", homeOrigin.getY());
        home.putInt("Z", homeOrigin.getZ());
        tag.put("Home", home);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        int ordinal = tag.getInt("BotState");
        if (ordinal >= 0 && ordinal < BrainState.values().length) {
            state = BrainState.values()[ordinal];
        }

        mineProgress = tag.getInt("MineProgress");
        buildIndex = tag.getInt("BuildIndex");

        if (tag.contains("Home")) {
            CompoundTag home = tag.getCompound("Home");
            homeOrigin = new BlockPos(home.getInt("X"), home.getInt("Y"), home.getInt("Z"));
        }
    }

    public void faceNearestPlayer() {
        LivingEntity p = level().getNearestPlayer(this, 32.0D);
        if (p != null) {
            getLookControl().setLookAt(p);
        }
    }
}
