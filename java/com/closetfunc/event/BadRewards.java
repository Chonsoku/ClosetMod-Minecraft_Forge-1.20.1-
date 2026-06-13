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

@SuppressWarnings("null")
public class BadRewards {
    private static final Map<Integer, ITypewriterReward> REWARDS = new HashMap<>();

    static {
        // ДЕНЬ 1
        REWARDS.put(2, new ITypewriterReward() {
            @Override
            public void tick(ServerPlayer player, ServerLevel level, int duration) {
                if (!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0, false, false));
                }
                
                if (!player.hasEffect(MobEffects.WEAKNESS)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, false));
                }

                if (!player.hasEffect(MobEffects.UNLUCK)) {
                    player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, duration, 0, false, false));
                }
            }

            @Override
            public void cleanup(ServerPlayer player, ServerLevel level) {
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.UNLUCK);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        });

        
        // ДЕНЬ 2
        REWARDS.put(4, new ITypewriterReward() {
            @Override
            public void tick(ServerPlayer player, ServerLevel level, int duration) {
                if (!player.getPersistentData().getBoolean("TypewriterHardcoreMode")) {
                    player.getPersistentData().putBoolean("TypewriterHardcoreMode", true);
                }
            }

            @Override
            public void cleanup(ServerPlayer player, ServerLevel level) {
                player.getPersistentData().remove("TypewriterHardcoreMode");
            }
        });

        // ДЕНЬ 3
        REWARDS.put(6, new ITypewriterReward() {
            @Override
            public void tick(ServerPlayer player, ServerLevel level, int duration) {
                if (!player.getPersistentData().getBoolean("TypewriterDay3InventoryStolen")) {
                    player.getPersistentData().putBoolean("TypewriterDay3InventoryStolen", true);

                    net.minecraft.nbt.ListTag inventoryList = new net.minecraft.nbt.ListTag();
                    player.getInventory().save(inventoryList);
                    
                    player.getPersistentData().put("StolenInventoryData", inventoryList);
                    player.getInventory().clearContent();
                    
                    net.minecraft.world.item.ItemStack closetStack = new net.minecraft.world.item.ItemStack(com.closetfunc.item.ModItems.CLOSET_ITEM.get(), 1);
                    player.getInventory().add(closetStack);
                }
            }

            @Override
            public void cleanup(ServerPlayer player, ServerLevel level) {
                if (player.getPersistentData().contains("StolenInventoryData")) {
                    net.minecraft.nbt.ListTag inventoryList = player.getPersistentData().getList("StolenInventoryData", 10);
                    
                    player.getInventory().clearContent();
                    player.getInventory().load(inventoryList);
                    
                    player.getPersistentData().remove("StolenInventoryData");
                    player.getPersistentData().remove("TypewriterDay3InventoryStolen");
                }
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
            player.setGameMode(GameType.SPECTATOR);
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.closet_mod.typewriter.death_hardcore")
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED).withStyle(net.minecraft.ChatFormatting.BOLD));
            
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 1.0F, 0.5F);
        }
    }
}