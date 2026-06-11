package com.closetfunc.event;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@SuppressWarnings("null")
public class GoodRewards {
    public static void execute(int rewardId, ServerPlayer player, ServerLevel level, int duration) {
        switch (rewardId) {
            case 1: // День 1
                executeAnnihilation(player, level);
                break;
                
            case 3: // День 2
                executeDayInvincibility(player, duration);
                break;
        }
    }

    private static void executeAnnihilation(ServerPlayer player, ServerLevel level) {
        List<?> entities = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(128.0D));
        for (Object obj : entities) {
            if (obj instanceof Monster monster && monster.isAlive()) {
                monster.hurt(level.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
        }
    }

    private static void executeDayInvincibility(ServerPlayer player, int duration) {
        if (!player.getPersistentData().getBoolean("TypewriterGodMode")) {
            player.getPersistentData().putBoolean("TypewriterGodMode", true);
        }
    }

    @SubscribeEvent
    public static void onGodModeDefend(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getPersistentData().getBoolean("TypewriterGodMode")) {
                event.setCanceled(true);
                event.setAmount(0.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onGodModeDefendAbsolute(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getPersistentData().getBoolean("TypewriterGodMode")) {
                event.setCanceled(true);
            }
        }
    }
}