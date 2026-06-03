package com.closetfunc.block_entity;

import com.closetfunc.MainCloset;
import com.closetfunc.block.ModBlocks;
import com.closetfunc.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import java.util.UUID;

@SuppressWarnings("null")
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MainCloset.MOD_ID);

    public static final RegistryObject<BlockEntityType<ClosetBlockEntity>> CLOSET_BE = BLOCK_ENTITIES.register("closet_be",
        () -> BlockEntityType.Builder.of(ClosetBlockEntity::new,
                ModBlocks.CLOSET_BLOCK.get(),
                ModBlocks.CLOSET_BATIM_BLOCK.get()
        ).build(null));

    // КЛАСС СУЩНОСТИ БЛОКА
    public static class ClosetBlockEntity extends BlockEntity {
        public UUID trappedPlayerId;

        public ClosetBlockEntity(BlockPos pos, BlockState state) {
            super(CLOSET_BE.get(), pos, state);
        }

        public void enterCloset(ServerPlayer player) {
            if (trappedPlayerId != null) return;
            this.trappedPlayerId = player.getUUID();
            this.setChanged();

            player.getPersistentData().putBoolean("IsInCloset", true);
            player.getPersistentData().putDouble("closet_exit_x", player.getX());
            player.getPersistentData().putDouble("closet_exit_y", player.getY());
            player.getPersistentData().putDouble("closet_exit_z", player.getZ());
            player.getPersistentData().putIntArray("closet_pos", new int[]{worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()});

            player.setNoGravity(true);
            player.teleportTo(worldPosition.getX() + 0.5, worldPosition.getY() + 0.01, worldPosition.getZ() + 0.5);
            player.setDeltaMovement(0, 0, 0);
            player.hurtMarked = true;

            Level currentLevel = this.getLevel();
            if (currentLevel != null) {
                boolean isBatim = this.getBlockState().is(ModBlocks.CLOSET_BATIM_BLOCK.get());
                net.minecraft.sounds.SoundEvent openSound = isBatim ? ModSounds.BATIM_OPEN.get() : net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN;

                currentLevel.playSound(player, worldPosition, openSound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                currentLevel.playSound(null, worldPosition, openSound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                currentLevel.scheduleTick(worldPosition, this.getBlockState().getBlock(), 25);
            }
        }

        public void exitCloset(ServerPlayer player) {
            player.getPersistentData().putBoolean("IsInCloset", false);
            player.setNoGravity(false);

            player.getPersistentData().putLong("ClosetCooldownUntil", player.level().getGameTime() + 180L);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DARKNESS, 180, 0, false, false
            ));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 180, 1, false, false
            ));

            Level currentLevel = this.getLevel();
            if (currentLevel != null) {
                BlockState state = currentLevel.getBlockState(worldPosition);
                if (state.hasProperty(com.closetfunc.block.ModBlocks.ClosetBlock.OPEN)) {
                    currentLevel.setBlock(worldPosition, state.setValue(com.closetfunc.block.ModBlocks.ClosetBlock.OPEN, true), 3);
                    BlockPos topPos = worldPosition.above();
                    BlockState topState = currentLevel.getBlockState(topPos);
                    if (topState.is(state.getBlock())) {
                        currentLevel.setBlock(topPos, topState.setValue(com.closetfunc.block.ModBlocks.ClosetBlock.OPEN, true), 3);
                    }

                    boolean isBatim = state.is(ModBlocks.CLOSET_BATIM_BLOCK.get());
                    net.minecraft.sounds.SoundEvent openSound = isBatim ? ModSounds.BATIM_OPEN.get() : net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN;

                    currentLevel.playSound(player, worldPosition, openSound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    currentLevel.playSound(null, worldPosition, openSound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                    currentLevel.scheduleTick(worldPosition, state.getBlock(), 25);
                }

                if (state.hasProperty(com.closetfunc.block.ModBlocks.ClosetBlock.FACING)) {
                    net.minecraft.core.Direction facing = state.getValue(com.closetfunc.block.ModBlocks.ClosetBlock.FACING);
                    BlockPos exitBlockPos = worldPosition.relative(facing);

                    double exitX = exitBlockPos.getX() + 0.5;
                    double exitY = worldPosition.getY() + 0.01;
                    double exitZ = exitBlockPos.getZ() + 0.5;

                    float yaw = facing.toYRot();
                    float pitch = player.getXRot();

                    player.teleportTo((net.minecraft.server.level.ServerLevel) currentLevel, exitX, exitY, exitZ, yaw, pitch);
                } else {
                    player.teleportTo(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() - 1.0);
                }
            }
            trappedPlayerId = null;
            this.setChanged();
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.player.level().isClientSide) return;
            net.minecraft.world.entity.player.Player player = event.player;

            if (player.getPersistentData().getBoolean("IsInCloset") && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
                int[] cPos = player.getPersistentData().getIntArray("closet_pos");
                BlockPos closetPos = (cPos.length == 3) ? new BlockPos(cPos[0], cPos[1], cPos[2]) : serverPlayer.blockPosition();

                if (serverPlayer.level().getBlockEntity(closetPos) instanceof ClosetBlockEntity be) {
                    be.exitCloset(serverPlayer);
                } else {
                    player.getPersistentData().putBoolean("IsInCloset", false);
                    player.setNoGravity(false);
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DARKNESS, 180, 0, false, false
                    ));
                }
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (trappedPlayerId != null) tag.putUUID("TrappedPlayer", trappedPlayerId);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.hasUUID("TrappedPlayer")) trappedPlayerId = tag.getUUID("TrappedPlayer");
        }

        @org.jetbrains.annotations.Nullable
        @Override
        public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
            return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public CompoundTag getUpdateTag() {
            CompoundTag tag = new CompoundTag();
            this.saveAdditional(tag);
            return tag;
        }

        @Override
        public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
            this.load(pkt.getTag() != null ? pkt.getTag() : new CompoundTag());
        }
    }
}
