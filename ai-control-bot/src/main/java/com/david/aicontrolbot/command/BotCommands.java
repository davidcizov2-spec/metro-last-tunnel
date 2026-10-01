package com.david.aicontrolbot.command;

import com.david.aicontrolbot.entity.ControlBotEntity;
import com.david.aicontrolbot.entity.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class BotCommands {
    private BotCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("aibot")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn").executes(c -> spawn(c.getSource())))
                .then(Commands.literal("forward").executes(c -> input(c.getSource(), 1, 0)))
                .then(Commands.literal("back").executes(c -> input(c.getSource(), -1, 0)))
                .then(Commands.literal("left").executes(c -> input(c.getSource(), 0, -1)))
                .then(Commands.literal("right").executes(c -> input(c.getSource(), 0, 1)))
                .then(Commands.literal("jump").executes(c -> jump(c.getSource())))
                .then(Commands.literal("stop").executes(c -> stop(c.getSource())))
                .then(Commands.literal("inv").executes(c -> openInventory(c.getSource()))));
    }

    private static ControlBotEntity nearest(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return null;

        ServerLevel level = player.serverLevel();

        return level.getEntitiesOfClass(
                        ControlBotEntity.class,
                        player.getBoundingBox().inflate(64.0D))
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
    }

    private static int spawn(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            return 0;
        }

        ControlBotEntity bot = ModEntities.CONTROL_BOT.get().create(player.serverLevel());

        if (bot == null) {
            return 0;
        }

        bot.moveTo(player.getX() + 2.0D, player.getY(), player.getZ() + 2.0D,
                player.getYRot(), 0.0F);

        bot.setCustomName(Component.literal("ChatBot"));
        bot.setCustomNameVisible(true);
        player.serverLevel().addFreshEntity(bot);

        source.sendSuccess(() -> Component.literal("AI-бот создан."), true);
        return 1;
    }

    private static int input(CommandSourceStack source, float forward, float strafe) {
        ControlBotEntity bot = nearest(source);

        if (bot == null) {
            source.sendFailure(Component.literal("Рядом нет AI-бота."));
            return 0;
        }

        bot.setInput(forward, strafe, 8);
        return 1;
    }

    private static int jump(CommandSourceStack source) {
        ControlBotEntity bot = nearest(source);

        if (bot == null) {
            source.sendFailure(Component.literal("Рядом нет AI-бота."));
            return 0;
        }

        bot.jumpNow();
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        ControlBotEntity bot = nearest(source);

        if (bot == null) {
            source.sendFailure(Component.literal("Рядом нет AI-бота."));
            return 0;
        }

        bot.stopInput();
        return 1;
    }

    private static int openInventory(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            return 0;
        }

        ControlBotEntity bot = nearest(source);

        if (bot == null) {
            source.sendFailure(Component.literal("Рядом нет AI-бота."));
            return 0;
        }

        bot.openInventory(player);
        return 1;
    }
}
