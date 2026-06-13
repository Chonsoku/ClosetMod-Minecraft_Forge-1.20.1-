package com.closetfunc.block;

import org.jetbrains.annotations.Nullable;

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

    public static final RegistryObject<Block> TYPEWRITER_BLOCK = BLOCKS.register("typewriter", 
        () -> new TypewriterBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F, 3.0F)
                .sound(SoundType.METAL)
                .noOcclusion()));


    // Класс для шкафа
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


    // Класс для печатной машинки
    public static class TypewriterBlock extends Block implements EntityBlock {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final BooleanProperty HAS_PAPER = BooleanProperty.create("has_paper");
        
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 7, 14);

        public TypewriterBlock(Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(HAS_PAPER, false));
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (hand == InteractionHand.OFF_HAND) {
                return InteractionResult.PASS;
            }

            net.minecraft.world.item.ItemStack heldItem = player.getItemInHand(hand);
            BlockEntity be = level.getBlockEntity(pos);
            
            if (!(be instanceof ModBlockEntities.TypewriterBlockEntity typewriterBe)) {
                return InteractionResult.FAIL;
            }

            if (heldItem.is(net.minecraft.world.item.Items.PAPER)) {
                if (!state.getValue(HAS_PAPER)) {
                    if (!level.isClientSide) {
                        level.setBlock(pos, state.setValue(HAS_PAPER, true), 3);
                        typewriterBe.insertedPaperCount = heldItem.getCount();
                        typewriterBe.setChanged();
                        if (!player.getAbilities().instabuild) heldItem.setCount(0);
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                } else {
                    if (typewriterBe.insertedPaperCount < 64) {
                        if (!level.isClientSide) {
                            BlockState oldState = level.getBlockState(pos);
                            if (!player.getAbilities().instabuild) {
                                heldItem.shrink(1);
                            }
                            typewriterBe.insertedPaperCount = Math.min(typewriterBe.insertedPaperCount + 2, 64);
                            typewriterBe.setChanged();
                            level.sendBlockUpdated(pos, oldState, oldState, 3);
                            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }

            if (state.getValue(HAS_PAPER)) {
                if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                    long currentWorldDayIndex = level.getDayTime() / 24000L;
                    var playerNBT = serverPlayer.getPersistentData();

                    if (!playerNBT.contains("TypewriterSurveyDay")) playerNBT.putInt("TypewriterSurveyDay", 1);
                    if (!playerNBT.contains("TypewriterDialogueStep")) playerNBT.putInt("TypewriterDialogueStep", 0);
                    if (!playerNBT.contains("TypewriterLastCompletedDay")) playerNBT.putLong("TypewriterLastCompletedDay", -1L);
                    if (!playerNBT.contains("TypewriterRewardType")) playerNBT.putInt("TypewriterRewardType", 0);
                    if (!playerNBT.contains("TypewriterFirstPageText")) playerNBT.putString("TypewriterFirstPageText", " ");

                    int pSurveyDay = playerNBT.getInt("TypewriterSurveyDay");
                    int pDialogueStep = playerNBT.getInt("TypewriterDialogueStep");
                    long pLastCompletedDay = playerNBT.getLong("TypewriterLastCompletedDay");

                    if (pDialogueStep == 3 && pLastCompletedDay != -1 && currentWorldDayIndex != pLastCompletedDay) {
                        if (pSurveyDay < com.closetfunc.event.SurveyManager.MAX_DAYS) {
                            pSurveyDay++;
                            playerNBT.putInt("TypewriterSurveyDay", pSurveyDay);
                        }
                        
                        pDialogueStep = 1;
                        playerNBT.putInt("TypewriterDialogueStep", pDialogueStep);
                        playerNBT.putInt("TypewriterRewardType", 0);
                        playerNBT.putInt("TypewriterCurrentEventId", 0);
                        playerNBT.putBoolean("TypewriterFirstAnswerWasBad", false);
                        
                        playerNBT.putString("TypewriterFirstPageText", " "); // Стираем текст
                    }
                    
                    if (pDialogueStep == 0) {
                        pDialogueStep = 1;
                        playerNBT.putInt("TypewriterDialogueStep", pDialogueStep);
                        playerNBT.putString("TypewriterFirstPageText", " ");
                    }

                    typewriterBe.surveyDay = pSurveyDay;
                    typewriterBe.dialogueStep = pDialogueStep;
                    typewriterBe.lastCompletedDay = pLastCompletedDay;
                    typewriterBe.rewardType = playerNBT.getInt("TypewriterRewardType");
                    typewriterBe.currentEventId = playerNBT.getInt("TypewriterCurrentEventId");
                    typewriterBe.firstAnswerWasBad = playerNBT.getBoolean("TypewriterFirstAnswerWasBad");
                    typewriterBe.pagesText[0] = playerNBT.getString("TypewriterFirstPageText");
                    
                    typewriterBe.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);

                    com.closetfunc.network.ModMessages.sendToPlayer(
                        new com.closetfunc.network.ModMessages.ClientboundOpenTypewriterPacket(
                            pos, typewriterBe.insertedPaperCount, pSurveyDay, typewriterBe.pagesText[0]
                        ), 
                        serverPlayer
                    );
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!level.isClientSide) {
                net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.translatable("chat.closet_mod.typewriter.need")
                        .withStyle(net.minecraft.ChatFormatting.RED);
                
                message.append(net.minecraft.network.chat.Component.translatable("chat.closet_mod.typewriter.paper")
                        .withStyle(net.minecraft.ChatFormatting.WHITE)
                        .withStyle(net.minecraft.ChatFormatting.UNDERLINE));

                message.append(net.minecraft.network.chat.Component.translatable("chat.closet_mod.typewriter.end")
                        .withStyle(net.minecraft.ChatFormatting.RED));

                player.sendSystemMessage(message);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
            if (!state.is(newState.getBlock())) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ModBlockEntities.TypewriterBlockEntity typewriterBe) {
                    int paperCount = typewriterBe.insertedPaperCount;
                    
                    if (paperCount > 0) {
                        net.minecraft.world.item.ItemStack paperStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER, paperCount);
                        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                            level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, paperStack
                        );
                        level.addFreshEntity(itemEntity);
                    }
                }
                super.onRemove(state, level, pos, newState, isMoving);
            }
        }

        @Override
        public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
            java.util.List<net.minecraft.world.item.ItemStack> drops = new java.util.ArrayList<>();
            drops.add(new net.minecraft.world.item.ItemStack(com.closetfunc.item.ModItems.TYPEWRITER_ITEM.get()));
            return drops;
        }

        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            return new com.closetfunc.block_entity.ModBlockEntities.TypewriterBlockEntity(pos, state);
        }

        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> builder) {
            builder.add(FACING, HAS_PAPER);
        }
    }
}

