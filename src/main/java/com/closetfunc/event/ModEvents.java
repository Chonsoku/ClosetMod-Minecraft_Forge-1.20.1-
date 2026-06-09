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
        if (state.is(ModBlocks.CLOSET_BLOCK.get()) || state.is(ModBlocks.CLOSET_BATIM_BLOCK.get()) || state.is(ModBlocks.CLOSET_BALDI_BLOCK.get())) {
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


    // ТЫ В ШКАФУ, КАК НЕУЯЗВИМЫЙ!!! ^.^
    @SubscribeEvent
    public static void InvinciblePlayer(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
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

                    // Шанс на появление пасхальных шкафов: 5%
                    if (level.random.nextFloat() > 0.05F) {
                        continue; 
                    }

                    // После выпадения шанса проверяем, в какой именно шкаф превратить (baldi/bendy)
                    boolean transformToBaldi = level.random.nextBoolean();

                    net.minecraft.core.Direction currentFacing = state.getValue(ModBlocks.ClosetBlock.FACING);

                    level.removeBlockEntity(immutablePos);
                    level.setBlock(immutablePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(immutablePos.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

                    BlockState newLowerState;
                    BlockState newTopState;
                    net.minecraft.sounds.SoundEvent spawnSound;

                    if (transformToBaldi) {
                        newLowerState = ModBlocks.CLOSET_BALDI_BLOCK.get().defaultBlockState()
                                .setValue(ModBlocks.ClosetBlock.FACING, currentFacing)
                                .setValue(ModBlocks.ClosetBlock.OPEN, false)
                                .setValue(ModBlocks.ClosetBlock.HALF, DoubleBlockHalf.LOWER);
                                
                        newTopState = ModBlocks.CLOSET_BALDI_BLOCK.get().defaultBlockState()
                                .setValue(ModBlocks.ClosetBlock.FACING, currentFacing)
                                .setValue(ModBlocks.ClosetBlock.OPEN, false)
                                .setValue(ModBlocks.ClosetBlock.HALF, DoubleBlockHalf.UPPER);
                        
                        spawnSound = ModSounds.BALDI_SPAWN.get(); // Скример при появление шкафа из Балди
                    } else {
                        newLowerState = ModBlocks.CLOSET_BATIM_BLOCK.get().defaultBlockState()
                                .setValue(ModBlocks.ClosetBlock.FACING, currentFacing)
                                .setValue(ModBlocks.ClosetBlock.OPEN, false)
                                .setValue(ModBlocks.ClosetBlock.HALF, DoubleBlockHalf.LOWER);
                                
                        newTopState = ModBlocks.CLOSET_BATIM_BLOCK.get().defaultBlockState()
                                .setValue(ModBlocks.ClosetBlock.FACING, currentFacing)
                                .setValue(ModBlocks.ClosetBlock.OPEN, false)
                                .setValue(ModBlocks.ClosetBlock.HALF, DoubleBlockHalf.UPPER);
                        
                        spawnSound = ModSounds.BATIM_SPAWN.get(); // Скример при появление шкафа из Бенди
                    }

                    level.setBlock(immutablePos, newLowerState, 3);
                    level.setBlock(immutablePos.above(), newTopState, 3);
                    
                    level.getChunkSource().blockChanged(immutablePos);
                    level.getChunkSource().blockChanged(immutablePos.above());

                    level.playSound(null, immutablePos.getX() + 0.5D, immutablePos.getY() + 0.5D, immutablePos.getZ() + 0.5D, 
                            spawnSound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                    // Для Балди-шкафа передаём: 20 тиков (1 сек), для Бенди-шкафа: 60 тиков (3 сек)
                    int duration = transformToBaldi ? 20 : 60;
                    applyScreamerEffects(player, level, duration);
                    break; 
                }
            }
        }
        
        // Таймер пасхалки на "Death Note" X.X
        long currentGameTime = level.getGameTime();

        if (player.getPersistentData().contains("DeathNoteTimeTarget")) {
            long deathTimeTarget = player.getPersistentData().getLong("DeathNoteTimeTarget");

            if (currentGameTime >= deathTimeTarget) {
                player.getPersistentData().remove("DeathNoteTimeTarget");

                net.minecraft.world.damagesource.DamageSource heartAttackSource = 
                    new net.minecraft.world.damagesource.DamageSource(
                        level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                    ) {
                        @Override
                        public net.minecraft.network.chat.Component getLocalizedDeathMessage(net.minecraft.world.entity.LivingEntity entity) {
                            return net.minecraft.network.chat.Component.translatable("death.attack.heart_attack", entity.getDisplayName());
                        }
                    };

                player.hurt(heartAttackSource, Float.MAX_VALUE);
            }
        }
    }
    
    @SuppressWarnings("unused")
    private static void applyScreamerEffects(ServerPlayer player, net.minecraft.world.level.Level level, int duration) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.DARKNESS, duration, 1, false, false
        ));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.BLINDNESS, duration, 0, false, false
        ));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration, 3, false, false
        ));
    }

    @SubscribeEvent
    public static void onWorldNightTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide() || event.level.getGameTime() % 20 != 0) return;
        
        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) event.level;
        long timeOfDay = level.getDayTime() % 24000L;
        
        // Ночь: от 13000 до 23800 тиков
        if (timeOfDay >= 13000L && timeOfDay <= 23800L) {
            long ticksUntilMorning = 24000L - timeOfDay;
            int duration = (int) ticksUntilMorning;

            for (ServerPlayer serverPlayer : level.players()) {
                BlockPos playerPos = serverPlayer.blockPosition();
                int r = 16;

                for (BlockPos targetPos : BlockPos.betweenClosed(playerPos.offset(-r, -4, -r), playerPos.offset(r, 4, r))) {
                    if (level.getBlockEntity(targetPos) instanceof ModBlockEntities.TypewriterBlockEntity typewriter && typewriter.rewardType > 0) {
                        
                        // РАСПРЕДЕЛЕНИЕ ПО КЛАССАМ:
                        if (typewriter.rewardType % 2 != 0) {
                            // Нечётные (1, 3, 5...) уходят в класс ХОРОШИХ исходов
                            GoodRewards.execute(typewriter.rewardType, serverPlayer, level, duration);
                        } else {
                            // Чётные (2, 4, 6...) уходят в класс ПЛОХИХ исходов
                            BadRewards.execute(typewriter.rewardType, serverPlayer, level, duration);
                        }
                    }
                }
            }
        }
    }
}
