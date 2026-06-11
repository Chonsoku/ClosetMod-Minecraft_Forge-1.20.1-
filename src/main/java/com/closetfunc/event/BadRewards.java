package com.closetfunc.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@SuppressWarnings("null")
public class BadRewards {
    public static void execute(int rewardId, ServerPlayer player, ServerLevel level, int duration) {
        switch (rewardId) {
            case 2: // День 1
                executeFatigue(player, duration);
                break;
                
            case 4: // День 2
                executeHardcoreMode(player, duration);
                break;
        }
    }

    private static void executeFatigue(ServerPlayer player, int duration) {
        if (!player.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, duration, 1, false, false
            ));
        }
    }

    private static void executeHardcoreMode(ServerPlayer player, int duration) {
        if (!player.getPersistentData().getBoolean("TypewriterHardcoreMode")) {
            player.getPersistentData().putBoolean("TypewriterHardcoreMode", true);
            
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                player.setGameMode(GameType.SURVIVAL);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerHeal(net.minecraftforge.event.entity.living.LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getPersistentData().getBoolean("TypewriterHardcoreMode")) {
                if (event.getAmount() == 1.0F && player.getFoodData().getFoodLevel() >= 18) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHardcoreDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getPersistentData().getBoolean("TypewriterHardcoreMode")) {
                event.setCanceled(true);
                
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.closet_mod.typewriter.death_hardcore")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED)
                        .withStyle(net.minecraft.ChatFormatting.BOLD));
                
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.setGameMode(GameType.SPECTATOR);
                
                player.getPersistentData().remove("TypewriterHardcoreMode");
                
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                        net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 1.0F, 0.5F);
            }
        }
    }
}