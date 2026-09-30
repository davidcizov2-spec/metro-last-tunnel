package com.david.metrolasttunnel.event;

import com.david.metrolasttunnel.item.GasMaskItem;
import com.david.metrolasttunnel.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerEvents {
    private static final String FLASHLIGHT_KEY = "metro_flashlight";
    private static final String FILTER_TICKS_KEY = "metro_filter_ticks";
    private static final String LAST_RADIATION_TICK_KEY = "metro_last_radiation_tick";

    public static boolean toggleFlashlight(Player player) {
        boolean active = !player.getPersistentData().getBoolean(FLASHLIGHT_KEY);
        if (active && !hasBattery(player)) return false;
        player.getPersistentData().putBoolean(FLASHLIGHT_KEY, active);
        return active;
    }

    private static boolean hasBattery(Player player) {
        return player.getInventory().countItem(ModItems.BATTERY.get()) > 0;
    }

    private static boolean consumeOne(Player player, net.minecraft.world.item.Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        long time = player.level().getGameTime();

        if (player.getPersistentData().getBoolean(FLASHLIGHT_KEY)) {
            if (hasBattery(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 40, 0, false, false, false));
                if (time % 6000 == 0 && consumeOne(player, ModItems.BATTERY.get())) {
                    player.displayClientMessage(Component.literal("Фонарик: батарейка разряжена"), true);
                }
            } else {
                player.getPersistentData().putBoolean(FLASHLIGHT_KEY, false);
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }

        long lastRadiation = player.getPersistentData().getLong(LAST_RADIATION_TICK_KEY);
        boolean inRadiation = time - lastRadiation <= 2;
        player.getPersistentData().putBoolean("metro_in_radiation", inRadiation);

        if (inRadiation) {
            boolean mask = GasMaskItem.isWorn(player);
            boolean hasFilter = player.getInventory().countItem(ModItems.FILTER.get()) > 0;

            if (!mask || !hasFilter) {
                if (time % 20 == 0) RadiationSystem.addRadiation(player, 1);
            }

            if (mask) {
                int used = player.getPersistentData().getInt(FILTER_TICKS_KEY) + 1;
                if (used >= 6000) {
                    player.getPersistentData().putInt(FILTER_TICKS_KEY, 0);
                    if (consumeOne(player, ModItems.FILTER.get())) {
                        player.displayClientMessage(Component.literal("Фильтр заменён"), true);
                    } else {
                        player.displayClientMessage(Component.literal("Фильтр закончился!"), true);
                    }
                } else {
                    player.getPersistentData().putInt(FILTER_TICKS_KEY, used);
                }
            }
        }

        if (time % 40 == 0) {
            int radiation = RadiationSystem.getRadiation(player);
            if (radiation > 0 && !inRadiation) RadiationSystem.removeRadiation(player, 1);
            if (radiation >= 100) player.hurt(player.damageSources().magic(), 1.0F);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player old = event.getOriginal();
        Player next = event.getEntity();
        next.getPersistentData().putInt("metro_radiation", old.getPersistentData().getInt("metro_radiation"));
        next.getPersistentData().putBoolean(FLASHLIGHT_KEY, old.getPersistentData().getBoolean(FLASHLIGHT_KEY));
        next.getPersistentData().putInt(FILTER_TICKS_KEY, old.getPersistentData().getInt(FILTER_TICKS_KEY));
    }
}
