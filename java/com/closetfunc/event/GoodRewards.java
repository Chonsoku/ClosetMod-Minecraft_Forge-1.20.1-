package com.closetfunc.event;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@SuppressWarnings("null")
public class GoodRewards {
    private static final Map<Integer, ITypewriterReward> REWARDS = new HashMap<>();

    static {
        // ДЕНЬ 1
        REWARDS.put(1, new ITypewriterReward() {
            @Override
            public void tick(ServerPlayer player, ServerLevel level, int duration) {
                var entities = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(128.0D));
                for (Monster monster : entities) {
                    if (monster.isAlive()) monster.hurt(level.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                }
            }
            @Override
            public void cleanup(ServerPlayer player, ServerLevel level) {} // Тут чистить нечего
        });

        // ДЕНЬ 2
        REWARDS.put(3, new ITypewriterReward() {
            @Override
            public void tick(ServerPlayer player, ServerLevel level, int duration) {
                player.getPersistentData().putBoolean("TypewriterGodMode", true);
            }
            @Override
            public void cleanup(ServerPlayer player, ServerLevel level) {
                player.getPersistentData().remove("TypewriterGodMode");
            }
        });

        // ДЕНЬ 3
        REWARDS.put(5, new ITypewriterReward() {
            @Override
            public void tick(ServerPlayer player, ServerLevel level, int duration) {
                if (!player.getPersistentData().getBoolean("TypewriterDay3GoodGiven")) {
                    player.getPersistentData().putBoolean("TypewriterDay3GoodGiven", true);

                    net.minecraft.world.item.ItemStack[] rewards = {
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_LEGGINGS),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_BOOTS),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_AXE),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_PICKAXE),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ELYTRA)
                    };

                    for (net.minecraft.world.item.ItemStack stack : rewards) {
                        if (!player.getInventory().add(stack)) {
                            player.drop(stack, false);
                        }
                    }
                }
            }

            @Override
            public void cleanup(ServerPlayer player, ServerLevel level) {
                // Очищаем флаг, чтобы в будущем (если нужно) механика могла отработать снова
                player.getPersistentData().remove("TypewriterDay3GoodGiven");
            }
        });
        
        // ДАЛЕЕ ТУТ БУДУТ ОСТАЛЬНЫЕ ДНИ...
    }

    public static void execute(int rewardId, ServerPlayer player, ServerLevel level, int duration) {
        ITypewriterReward reward = REWARDS.get(rewardId);
        if (reward != null) {
            reward.tick(player, level, duration);
        }
    }

    public static void cleanup(int rewardId, ServerPlayer player, ServerLevel level) {
        ITypewriterReward reward = REWARDS.get(rewardId);
        if (reward != null) {
            reward.cleanup(player, level);
        }
    }

    @SubscribeEvent
    public static void onGodModeDefend(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && player.getPersistentData().getBoolean("TypewriterGodMode")) {
            event.setCanceled(true);
            event.setAmount(0.0F);
        }
    }

    @SubscribeEvent
    public static void onGodModeDefendAbsolute(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player && player.getPersistentData().getBoolean("TypewriterGodMode")) {
            event.setCanceled(true);
        }
    }
}
