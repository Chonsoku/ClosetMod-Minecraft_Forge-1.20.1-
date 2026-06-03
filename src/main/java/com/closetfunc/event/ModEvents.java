package com.closetfunc.event;

import com.closetfunc.block.ModBlocks;
import com.closetfunc.block_entity.ModBlockEntities;
import com.closetfunc.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@SuppressWarnings("null")
public class ModEvents {
    @SubscribeEvent
    public static void onMobTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player) {
            long currentTime = player.level().getGameTime();
            long cooldownUntil = player.getPersistentData().getLong("ClosetCooldownUntil");
            if (player.getPersistentData().getBoolean("IsInCloset") || currentTime < cooldownUntil) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickDefense(PlayerInteractEvent.LeftClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.is(ModBlocks.CLOSET_BLOCK.get()) || state.is(ModBlocks.CLOSET_BATIM_BLOCK.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMobTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof Mob mob) {
            net.minecraft.world.entity.LivingEntity target = mob.getTarget();
            if (target instanceof Player player) {
                long currentTime = player.level().getGameTime();
                long cooldownUntil = player.getPersistentData().getLong("ClosetCooldownUntil");
                if (player.getPersistentData().getBoolean("IsInCloset") || currentTime < cooldownUntil) {
                    mob.setTarget(null);
                    if (mob instanceof Monster) {
                        mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
                        mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
                    }
                    mob.getNavigation().stop();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getPersistentData().getBoolean("IsInCloset")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerProximityCheck(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 20 != 0) return;

        ServerPlayer player = (ServerPlayer) event.player;
        if (player.getPersistentData().getBoolean("IsInCloset")) return;

        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        int radius = 6;
        for (BlockPos targetPos : BlockPos.betweenClosed(playerPos.offset(-radius, -2, -radius), playerPos.offset(radius, 2, radius))) {
            BlockPos immutablePos = targetPos.immutable();
            BlockState state = level.getBlockState(immutablePos);

            if (state.is(ModBlocks.CLOSET_BLOCK.get()) && state.getValue(ModBlocks.ClosetBlock.HALF) == DoubleBlockHalf.LOWER) {

                BlockEntity be = level.getBlockEntity(immutablePos);
                if (be instanceof ModBlockEntities.ClosetBlockEntity closetBe) {
                    if (closetBe.trappedPlayerId != null) continue;

                    if (closetBe.getPersistentData().getBoolean("CheckedForBatim")) {
                        continue;
                    }

                    closetBe.getPersistentData().putBoolean("CheckedForBatim", true);
                    closetBe.setChanged();

                    // Шанс на выпадение шкафа из BatIM: 14%:
                    if (level.random.nextFloat() > 0.14F) {
                        continue;
                    }

                    net.minecraft.core.Direction currentFacing = state.getValue(ModBlocks.ClosetBlock.FACING);

                    level.removeBlockEntity(immutablePos);
                    level.setBlock(immutablePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(immutablePos.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

                    BlockState newLowerState = ModBlocks.CLOSET_BATIM_BLOCK.get().defaultBlockState()
                            .setValue(ModBlocks.ClosetBlock.FACING, currentFacing)
                            .setValue(ModBlocks.ClosetBlock.OPEN, false)
                            .setValue(ModBlocks.ClosetBlock.HALF, DoubleBlockHalf.LOWER);

                    BlockState newTopState = ModBlocks.CLOSET_BATIM_BLOCK.get().defaultBlockState()
                            .setValue(ModBlocks.ClosetBlock.FACING, currentFacing)
                            .setValue(ModBlocks.ClosetBlock.OPEN, false)
                            .setValue(ModBlocks.ClosetBlock.HALF, DoubleBlockHalf.UPPER);

                    level.setBlock(immutablePos, newLowerState, 3);
                    level.setBlock(immutablePos.above(), newTopState, 3);

                    level.getChunkSource().blockChanged(immutablePos);
                    level.getChunkSource().blockChanged(immutablePos.above());

                    level.playSound(null, immutablePos.getX() + 0.5D, immutablePos.getY() + 0.5D, immutablePos.getZ() + 0.5D,
                            ModSounds.BATIM_SPAWN.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                    applyScreamerEffects(player, level);

                    break;
                }
            }
        }
    }

    private static void applyScreamerEffects(ServerPlayer player, net.minecraft.world.level.Level level) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.DARKNESS, 60, 1, false, false
        ));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0, false, false
        ));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, false
        ));
    }
}
