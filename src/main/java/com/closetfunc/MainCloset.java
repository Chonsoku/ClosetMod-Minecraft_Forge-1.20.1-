package com.closetfunc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;


@Mod(MainCloset.MOD_ID)
@SuppressWarnings({"removal", "null", "unused"})
public class MainCloset {
    public static final String MOD_ID = "closet_mod";
    

    // --- РЕГИСТРАЦИЯ КОМПОНЕНТОВ ---
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES = 
        DeferredRegister.create(ForgeRegistries.FEATURES, MOD_ID);


    public static final RegistryObject<Block> CLOSET_BLOCK = BLOCKS.register("closet", 
        () -> new ClosetBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(-1.0F, 3600000.0F)
                .noOcclusion()
                .dynamicShape()));
    public static final RegistryObject<Item> CLOSET_ITEM = ITEMS.register("closet", () -> new BlockItem(CLOSET_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<ClosetBlockEntity>> CLOSET_BE = BLOCK_ENTITIES.register("closet_be", 
            () -> BlockEntityType.Builder.of(ClosetBlockEntity::new, CLOSET_BLOCK.get()).build(com.mojang.datafixers.DSL.remainderType()));
    public static final RegistryObject<net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> CLOSET_FEATURE = 
        FEATURES.register("closet_feature", () -> new ClosetFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
            

    public MainCloset() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        FEATURES.register(bus);

        MinecraftForge.EVENT_BUS.register(MainCloset.class);
        MinecraftForge.EVENT_BUS.register(ClosetBlockEntity.class);
        MinecraftForge.EVENT_BUS.register(HiderEvents.class);

        if (FMLEnvironment.dist.isClient()) {
            ClosetClient.init();
        }

        bus.addListener(this::addCreative);
    }
    private void addCreative(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) {
        event.accept(CLOSET_ITEM.get());
        }
    }
    
    

    // --- КЛАСС БЛОКА ШКАФА ---
    public static class ClosetBlock extends Block implements EntityBlock {
        public static final net.minecraft.world.level.block.state.properties.BooleanProperty OPEN = 
                net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN;
        public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = 
                net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
        public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.world.level.block.state.properties.DoubleBlockHalf> HALF = 
                net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;
        private static final VoxelShape LOWER_SHAPE = Block.box(1, 0, 1, 15, 32, 15);
        private static final VoxelShape UPPER_SHAPE = Block.box(1, -16, 1, 15, 16, 15);
        
        public ClosetBlock(Properties properties) { 
            super(properties); 
            this.registerDefaultState(this.stateDefinition.any()
                .setValue(OPEN, false)
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        }
        
        @Override
        public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
            BlockPos blockpos = ctx.getClickedPos();
            Level level = ctx.getLevel();
            if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(ctx)) {
                return this.defaultBlockState()
                    .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                    .setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
            }
            return null;
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
            level.setBlock(pos.above(), state.setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (!level.isClientSide && player.isCreative()) {
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf half = state.getValue(HALF);
                if (half == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
                    BlockPos blockpos = pos.below();
                    BlockState blockstate = level.getBlockState(blockpos);
                    if (blockstate.is(state.getBlock()) && blockstate.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                        level.setBlock(blockpos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
                        level.levelEvent(player, 2001, blockpos, Block.getId(blockstate));
                    }
                }
            }
            super.playerWillDestroy(level, pos, state, player);
        }
        
        @Override 
        public VoxelShape getShape(BlockState state, BlockGetter lvl, BlockPos pos, CollisionContext ctx) { 
            return state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE; 
        }
        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return net.minecraft.world.phys.shapes.Shapes.empty();
        }

        @Override
        public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
            return state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER 
                ? net.minecraft.world.level.block.RenderShape.MODEL 
                : net.minecraft.world.level.block.RenderShape.INVISIBLE;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER 
                    && hit.getDirection() == net.minecraft.core.Direction.UP) {
                return InteractionResult.PASS;
            }

            BlockPos mainPos = state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockState mainState = level.getBlockState(mainPos);

            if (!mainState.is(this)) {
                return InteractionResult.FAIL;
            }

            if (level.isClientSide) {
                if (level.getBlockEntity(mainPos) instanceof ClosetBlockEntity) player.getPersistentData().putBoolean("IsInCloset", true);
                return InteractionResult.SUCCESS;
            }
            
            if (level.getBlockEntity(mainPos) instanceof ClosetBlockEntity be && be.trappedPlayerId == null) {
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
        public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
            net.minecraft.world.level.block.state.properties.DoubleBlockHalf half = state.getValue(HALF);
            if (direction.getAxis() == net.minecraft.core.Direction.Axis.Y && (half == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) == (direction == net.minecraft.core.Direction.UP)) {
                if (!neighborState.is(this) || neighborState.getValue(HALF) == half) {
                    return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                }
            }
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, OPEN, HALF);
        }

        @Nullable @Override 
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { 
            return state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? new ClosetBlockEntity(pos, state) : null; 
        }
        
        @Nullable @Override 
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level lvl, BlockState st, BlockEntityType<T> type) {
            if (st.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) return null;
            if (type == CLOSET_BE.get()) {
                return (level, pos, state, blockEntity) -> {
                    if (level.isClientSide || !(blockEntity instanceof ClosetBlockEntity be) || be.trappedPlayerId == null) return;
                    
                    ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(be.trappedPlayerId);
                    if (player == null) { 
                        be.trappedPlayerId = null; 
                        level.setBlock(pos, state.setValue(OPEN, false), 3);
                        BlockPos topPos = pos.above();
                        BlockState topState = level.getBlockState(topPos);
                        if (topState.is(this)) level.setBlock(topPos, topState.setValue(OPEN, false), 3);
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
            if (!level.isClientSide && state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                BlockPos topPos = pos.above();
                if (level.getBlockState(topPos).canBeReplaced()) {
                    level.setBlock(topPos, state.setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
                }
            }
            super.onPlace(state, level, pos, oldState, isMoving);
        }

        @Override
        public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
            if (state.getValue(OPEN)) {
                level.setBlock(pos, state.setValue(OPEN, false), 3);
                BlockPos otherPos = state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? pos.above() : pos.below();
                BlockState otherState = level.getBlockState(otherPos);
                if (otherState.is(this)) {
                    level.setBlock(otherPos, otherState.setValue(OPEN, false), 3);
                }

                level.playSound(null, pos, 
                    net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE, 
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }


    // --- КЛАСС СУЩНОСТИ БЛОКА (BLOCK ENTITY) ---
    public static class ClosetBlockEntity extends BlockEntity {
        public UUID trappedPlayerId;
        public ClosetBlockEntity(BlockPos pos, BlockState state) { super(CLOSET_BE.get(), pos, state); }

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
                currentLevel.playSound(null, worldPosition, 
                    net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN, 
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                currentLevel.scheduleTick(worldPosition, MainCloset.CLOSET_BLOCK.get(), 25);
            }
        }

        public void exitCloset(ServerPlayer player) {
            player.getPersistentData().putBoolean("IsInCloset", false);
            player.setNoGravity(false);    
            player.getPersistentData().putLong("ClosetCooldownUntil", player.level().getGameTime() + 200L);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DARKNESS, 200, 0, false, false
            ));
            Level currentLevel = this.getLevel();
            if (currentLevel != null) {
                BlockState state = currentLevel.getBlockState(worldPosition);
                if (state.hasProperty(ClosetBlock.OPEN)) {
                    currentLevel.setBlock(worldPosition, state.setValue(ClosetBlock.OPEN, true), 3);
                    BlockPos topPos = worldPosition.above();
                    BlockState topState = currentLevel.getBlockState(topPos);
                    if (topState.is(state.getBlock())) {
                        currentLevel.setBlock(topPos, topState.setValue(ClosetBlock.OPEN, true), 3);
                    }
                    currentLevel.playSound(null, worldPosition, 
                        net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE, 
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    currentLevel.scheduleTick(worldPosition, state.getBlock(), 25);
                }
                if (state.hasProperty(ClosetBlock.FACING)) {
                    net.minecraft.core.Direction facing = state.getValue(ClosetBlock.FACING);
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

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
            Player player = event.player;
            
            if (player.getPersistentData().getBoolean("IsInCloset") && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
                int[] cPos = player.getPersistentData().getIntArray("closet_pos");
                BlockPos closetPos = (cPos.length == 3) ? new BlockPos(cPos[0], cPos[1], cPos[2]) : serverPlayer.blockPosition();
                
                if (serverPlayer.level().getBlockEntity(closetPos) instanceof ClosetBlockEntity be) {
                    be.exitCloset(serverPlayer);
                } else { 
                    player.getPersistentData().putBoolean("IsInCloset", false); 
                    player.setNoGravity(false); 
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DARKNESS, 200, 0, false, false
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
    }



    // --- ЛОГИКА СКРЫТИЯ ОТ ДВЕЛЛЕРОВ И ДРУГИХ ВРАЖДЕБНЫХ МОБОВ ---
    public static class HiderEvents {
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
            if (event.getLevel().getBlockState(event.getPos()).is(MainCloset.CLOSET_BLOCK.get())) {
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

        // ТЫ КАК INVINCIBLE ВНУТРИ ШКАФА! (НЕУЯЗВИМОСТЬ)
        @SubscribeEvent
        public static void onPlayerHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
            if (event.getEntity() instanceof Player player) {
                if (player.getPersistentData().getBoolean("IsInCloset")) {
                    event.setCanceled(true);
                }
            }
        }
    }
}
