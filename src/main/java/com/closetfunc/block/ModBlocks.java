package com.closetfunc.block;

import com.closetfunc.MainCloset;
import com.closetfunc.block_entity.ModBlockEntities;
import com.closetfunc.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MainCloset.MOD_ID);

    public static final RegistryObject<Block> CLOSET_BLOCK = BLOCKS.register("closet",
        () -> new ClosetBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .dynamicShape()));

    public static final RegistryObject<Block> CLOSET_BATIM_BLOCK = BLOCKS.register("closet_batim",
        () -> new ClosetBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .dynamicShape()));

    public static final RegistryObject<Block> CLOSET_BALDI_BLOCK = BLOCKS.register("closet_baldi", 
    () -> new ClosetBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .strength(-1.0F, 3600000.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .dynamicShape()));

    public static class ClosetBlock extends Block implements EntityBlock {
        public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

        private static final VoxelShape LOWER_SHAPE = Block.box(1, 0, 1, 15, 32, 15);
        private static final VoxelShape UPPER_SHAPE = Block.box(1, -16, 1, 15, 16, 15);

        public ClosetBlock(Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any()
                .setValue(OPEN, false)
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
        }

        @Override
        public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
            BlockPos blockpos = ctx.getClickedPos();
            Level level = ctx.getLevel();
            if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(ctx)) {
                return this.defaultBlockState()
                    .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                    .setValue(HALF, DoubleBlockHalf.LOWER);
            }
            return null;
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (!level.isClientSide && player.isCreative()) {
                DoubleBlockHalf half = state.getValue(HALF);
                if (half == DoubleBlockHalf.UPPER) {
                    BlockPos blockpos = pos.below();
                    BlockState blockstate = level.getBlockState(blockpos);
                    if (blockstate.is(state.getBlock()) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER) {
                        level.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 35);
                        level.levelEvent(player, 2001, blockpos, Block.getId(blockstate));
                    }
                }
            }
            super.playerWillDestroy(level, pos, state, player);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter lvl, BlockPos pos, CollisionContext ctx) {
            return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
        }

        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? RenderShape.MODEL
                : RenderShape.INVISIBLE;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (state.getValue(HALF) == DoubleBlockHalf.UPPER && hit.getDirection() == Direction.UP) {
                return InteractionResult.PASS;
            }

            BlockPos mainPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockState mainState = level.getBlockState(mainPos);

            if (!mainState.is(this)) {
                return InteractionResult.FAIL;
            }

            if (level.isClientSide) {
                if (level.getBlockEntity(mainPos) instanceof ModBlockEntities.ClosetBlockEntity) {
                    player.getPersistentData().putBoolean("IsInCloset", true);
                }
                return InteractionResult.SUCCESS;
            }

            if (level.getBlockEntity(mainPos) instanceof ModBlockEntities.ClosetBlockEntity be && be.trappedPlayerId == null) {
                if (player instanceof ServerPlayer serverPlayer) {
                    level.setBlock(mainPos, mainState.setValue(OPEN, true), 3);
                    BlockPos topPos = mainPos.above();
                    BlockState topState = level.getBlockState(topPos);
                    if (topState.is(this)) {
                        level.setBlock(topPos, topState.setValue(OPEN, true), 3);
                    }
                    be.enterCloset(serverPlayer);
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
            DoubleBlockHalf half = state.getValue(HALF);
            if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
                if (!neighborState.is(this) || neighborState.getValue(HALF) == half) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, OPEN, HALF);
        }

        @Nullable @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ModBlockEntities.ClosetBlockEntity(pos, state) : null;
        }

        @Nullable @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level lvl, BlockState st, BlockEntityType<T> type) {
            if (st.getValue(HALF) == DoubleBlockHalf.UPPER) return null;
            if (type == ModBlockEntities.CLOSET_BE.get()) {
                return (level, pos, state, blockEntity) -> {
                    if (level.isClientSide || !(blockEntity instanceof ModBlockEntities.ClosetBlockEntity be) || be.trappedPlayerId == null) return;

                    ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(be.trappedPlayerId);
                    if (player == null) {
                        be.trappedPlayerId = null;
                        level.setBlock(pos, state.setValue(OPEN, false), 3);
                        BlockPos topPos = pos.above();
                        BlockState topState = level.getBlockState(topPos);
                        if (topState.is(state.getBlock())) level.setBlock(topPos, topState.setValue(OPEN, false), 3);
                        be.setChanged();
                        return;
                    }

                    if (player.distanceToSqr(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5) > 0.5) {
                        player.teleportTo(pos.getX() + 0.5, pos.getY() + 0.01, pos.getZ() + 0.5);
                    }
                };
            }
            return null;
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
            if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockPos topPos = pos.above();
                if (level.getBlockState(topPos).canBeReplaced()) {
                    level.setBlock(topPos, state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
                }
            }
            super.onPlace(state, level, pos, oldState, isMoving);
        }

        @Override
        public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
            if (state.getValue(OPEN)) {
                level.setBlock(pos, state.setValue(OPEN, false), 3);

                BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
                BlockState otherState = level.getBlockState(otherPos);
                if (otherState.is(this)) {
                    level.setBlock(otherPos, otherState.setValue(OPEN, false), 3);
                }

                boolean isBatim = state.is(ModBlocks.CLOSET_BATIM_BLOCK.get());
                boolean isBaldi = state.is(ModBlocks.CLOSET_BALDI_BLOCK.get());

                net.minecraft.sounds.SoundEvent closeSound;
                if (isBatim) {
                    closeSound = ModSounds.BATIM_CLOSE.get();
                } else if (isBaldi) {
                    closeSound = ModSounds.BALDI_CLOSE.get();
                } else {
                    closeSound = net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE;
                }

                if (closeSound != null) {
                    level.playSound(null, pos, closeSound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }
}
