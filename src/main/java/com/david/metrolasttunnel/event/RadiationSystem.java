package com.david.metrolasttunnel.event;

import net.minecraft.world.entity.player.Player;

public final class RadiationSystem {
    private static final String KEY = "metro_radiation";
    private static final int MAX = 150;

    private RadiationSystem() {}

    public static int getRadiation(Player player) { return player.getPersistentData().getInt(KEY); }

    public static void setRadiation(Player player, int value) {
        player.getPersistentData().putInt(KEY, Math.max(0, Math.min(MAX, value)));
    }

    public static void addRadiation(Player player, int amount) { setRadiation(player, getRadiation(player) + amount); }
    public static void removeRadiation(Player player, int amount) { setRadiation(player, getRadiation(player) - amount); }
}
