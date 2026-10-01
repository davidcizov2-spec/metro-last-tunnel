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
        d.register(Commands.literal("aibot").requires(s -> s.hasPermission(2))
            .then(Commands.literal("spawn").executes(c -> spawn(c.getSource())))
            .then(Commands.literal("forward").executes(c -> input(c.getSource(),1,0)))
            .then(Commands.literal("back").executes(c -> input(c.getSource(),-1,0)))
            .then(Commands.literal("left").executes(c -> input(c.getSource(),0,-1)))
            .then(Commands.literal("right").executes(c -> input(c.getSource(),0,1)))
            .then(Commands.literal("jump").executes(c -> jump(c.getSource())))
            .then(Commands.literal("stop").executes(c -> stop(c.getSource()))));
    }
    private static ControlBotEntity nearest(CommandSourceStack s) {
        ServerPlayer p = s.getPlayer();
        if (p == null) return null;
        ServerLevel l = p.serverLevel();
        return l.getEntitiesOfClass(ControlBotEntity.class, p.getBoundingBox().inflate(64))
                .stream().min((a,b)->Double.compare(a.distanceToSqr(p),b.distanceToSqr(p))).orElse(null);
    }
    private static int spawn(CommandSourceStack s) {
        ServerPlayer p;
        try { p = s.getPlayerOrException(); } catch (Exception e) { return 0; }
        ControlBotEntity b = ModEntities.CONTROL_BOT.get().create(p.serverLevel());
        if (b == null) return 0;
        b.moveTo(p.getX()+2,p.getY(),p.getZ()+2,p.getYRot(),0);
        b.setCustomName(Component.literal("ChatBot")); b.setCustomNameVisible(true);
        p.serverLevel().addFreshEntity(b);
        s.sendSuccess(() -> Component.literal("AI-бот создан."), true);
        return 1;
    }
    private static int input(CommandSourceStack s,float f,float st) { ControlBotEntity b=nearest(s); if(b==null){s.sendFailure(Component.literal("Рядом нет AI-бота."));return 0;} b.setInput(f,st,8); return 1; }
    private static int jump(CommandSourceStack s) { ControlBotEntity b=nearest(s); if(b==null)return 0; b.jumpNow(); return 1; }
    private static int stop(CommandSourceStack s) { ControlBotEntity b=nearest(s); if(b==null)return 0; b.stopInput(); return 1; }
}
