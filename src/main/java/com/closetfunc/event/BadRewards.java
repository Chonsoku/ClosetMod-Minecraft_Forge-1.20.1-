package com.closetfunc.event;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BadRewards {
    private static final Map<Integer, ITypewriterReward> REWARDS = new HashMap<>();

    static {
        // ДЕНЬ 1
        REWARDS.put(2, new ITypewriterReward() {
            @Override
            public void tick(ServerPlayer player, ServerLevel level, int duration) {
                if (!player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 1, false, false));
                }
            }
            @Override
            public void cleanup(ServerPlayer player, ServerLevel level) {
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
            }
        });

        // ДЕНЬ 2
        REWARDS.put(4, new ITypewriterReward() {
            @Override
            public void tick(ServerPlayer player, ServerLevel level, int duration) {
                if (!player.getPersistentData().getBoolean("TypewriterHardcoreMode")) {
                    player.getPersistentData().putBoolean("TypewriterHardcoreMode", true);
                    if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                        player.setGameMode(GameType.SURVIVAL);
                    }
                }
                
                // Буквально отключаем естественную регенерацию здоровья через ванильный игровой фрукт (игровое правило)
                // Это заблокирует реген от еды на 100% без костылей с эвентами!
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION).set(false, level.getServer());
            }
            @Override
            public void cleanup(ServerPlayer player, ServerLevel level) {
                player.getPersistentData().remove("TypewriterHardcoreMode");
                // Возвращаем регенерацию обратно на следующий день
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION).set(true, level.getServer());
            }
        });

        
        // ДАЛЕЕ ТУТ БУДУТ ОСТАЛЬНЫЕ ДНИ...
    }

    public static void execute(int rewardId, ServerPlayer player, ServerLevel level, int duration) {
        ITypewriterReward reward = REWARDS.get(rewardId);
        if (reward != null) reward.tick(player, level, duration);
    }

    public static void cleanup(int rewardId, ServerPlayer player, ServerLevel level) {
        ITypewriterReward reward = REWARDS.get(rewardId);
        if (reward != null) reward.cleanup(player, level);
    }

    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getPersistentData().getBoolean("TypewriterHardcoreMode")) {
            if (event.getAmount() == 1.0F && player.getFoodData().getFoodLevel() >= 18) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onHardcoreDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getPersistentData().getBoolean("TypewriterHardcoreMode")) {
            // НЕ отменяем эвент! Даем игроку умереть, чтобы Майнкрафт отработал логику удаления мира,
            // ЕСЛИ синглплеер изначально был создан как хардкор.
            // Но чтобы забанить игрока на обычном сервере/мире, мы принудительно переведем его в спектаторы при респавне:
            player.setGameMode(GameType.SPECTATOR);
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.closet_mod.typewriter.death_hardcore")
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED).withStyle(net.minecraft.ChatFormatting.BOLD));
            
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 1.0F, 0.5F);
        }
    }
}