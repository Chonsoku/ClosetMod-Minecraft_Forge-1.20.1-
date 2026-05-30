package com.closetfunc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
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
import net.minecraftforge.event.level.BlockEvent;
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
public class MainCloset {
    public static final String MOD_ID = "closet_mod";

    // --- РЕГИСТРАЦИЯ КОМПОНЕНТОВ ---
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final RegistryObject<Block> CLOSET_BLOCK = BLOCKS.register("closet", 
        () -> new ClosetBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(-1.0F, 3600000.0F)
                .noOcclusion()
                .dynamicShape()));
    public static final RegistryObject<Item> CLOSET_ITEM = ITEMS.register("closet", () -> new BlockItem(CLOSET_BLOCK.get(), new Item.Properties()));
    
    public static final RegistryObject<BlockEntityType<ClosetBlockEntity>> CLOSET_BE = BLOCK_ENTITIES.register("closet_be", 
            () -> BlockEntityType.Builder.of(ClosetBlockEntity::new, CLOSET_BLOCK.get()).build(com.mojang.datafixers.DSL.remainderType()));

    public MainCloset() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);

        MinecraftForge.EVENT_BUS.register(MainCloset.class);
        MinecraftForge.EVENT_BUS.register(ClosetBlockEntity.class);
        MinecraftForge.EVENT_BUS.register(HiderEvents.class);

        if (FMLEnvironment.dist.isClient()) {
            ClosetClient.init();
        }
    }


    // --- КЛАСС БЛОКА ШКАФА ---
    public static class ClosetBlock extends Block implements EntityBlock {
        private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 32, 15);
        public ClosetBlock(Properties properties) { super(properties); }
        @Override public VoxelShape getShape(BlockState state, BlockGetter lvl, BlockPos pos, CollisionContext ctx) { return SHAPE; }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (level.isClientSide) {
                if (level.getBlockEntity(pos) instanceof ClosetBlockEntity) player.getPersistentData().putBoolean("IsInCloset", true);
                return InteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof ClosetBlockEntity be && be.trappedPlayerId == null) {
                if (player instanceof ServerPlayer serverPlayer) be.enterCloset(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }

        @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ClosetBlockEntity(pos, state); }
        @Nullable @Override 
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level lvl, BlockState st, BlockEntityType<T> type) {
            if (type == CLOSET_BE.get()) {
                return (level, pos, state, blockEntity) -> {
                    if (level.isClientSide || !(blockEntity instanceof ClosetBlockEntity be) || be.trappedPlayerId == null) return;
                    
                    ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(be.trappedPlayerId);
                    if (player == null) { 
                        be.trappedPlayerId = null; 
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
    }

    // --- КЛАСС СУЩЕСТВЕННОСТИ БЛОКА (BLOCK ENTITY) ---
    public static class ClosetBlockEntity extends BlockEntity {
        public UUID trappedPlayerId;
        public ClosetBlockEntity(BlockPos pos, BlockState state) { super(CLOSET_BE.get(), pos, state); }

        public void enterCloset(ServerPlayer player) {
            if (trappedPlayerId != null) return;
            this.trappedPlayerId = player.getUUID();
            this.setChanged();

            player.getPersistentData().putBoolean("IsInCloset", true);
            player.getPersistentData().putIntArray("closet_exit_pos", new int[]{player.getBlockX(), player.getBlockY(), player.getBlockZ()});
            player.setNoGravity(true);
            player.teleportTo(worldPosition.getX() + 0.5, worldPosition.getY() + 0.01, worldPosition.getZ() + 0.5);
            player.setDeltaMovement(0, 0, 0);
            player.hurtMarked = true;
        }

        public void exitCloset(ServerPlayer player) {
            player.getPersistentData().putBoolean("IsInCloset", false);
            player.setNoGravity(false);    
            player.getPersistentData().putLong("ClosetCooldownUntil", player.level().getGameTime() + 150L);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DARKNESS, 150, 0, false, false
            ));

            int[] exitPos = player.getPersistentData().getIntArray("closet_exit_pos");
            if (exitPos.length == 3) player.teleportTo(exitPos[0] + 0.5, exitPos[1] + 0.5, exitPos[2] + 0.5);
            else player.teleportTo(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() - 1.0);
            trappedPlayerId = null;
            this.setChanged();
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
            Player player = event.player;
            if (player.getPersistentData().getBoolean("IsInCloset") && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.level().getBlockEntity(serverPlayer.blockPosition()) instanceof ClosetBlockEntity be) {
                    be.exitCloset(serverPlayer);
                } else { 
                    player.getPersistentData().putBoolean("IsInCloset", false); 
                    player.setNoGravity(false); 
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DARKNESS, 150, 0, false, false
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
        
        // ↓Жоская прикалюха↓
        @SubscribeEvent
        public static void onPlayerStepOnCloset(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
            if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide) {
                BlockPos posUnder = player.blockPosition().below(2);
                if (player.level().getBlockState(posUnder).is(MainCloset.CLOSET_BLOCK.get()) 
                    && !player.isPassenger() 
                    && !player.isShiftKeyDown()
                    && !player.getPersistentData().getBoolean("IsInCloset")) {
                    ArmorStand chair = new ArmorStand(player.level(), posUnder.getX() + 0.5, posUnder.getY() + 2.0, posUnder.getZ() + 0.5);
                    chair.setInvisible(true);
                    chair.setNoGravity(true);
                    chair.getEntityData().set(net.minecraft.world.entity.decoration.ArmorStand.DATA_CLIENT_FLAGS, (byte)(0x01 | 0x10));
                    
                    chair.getPersistentData().putBoolean("ClosetChair", true); // Наша метка
                    
                    player.level().addFreshEntity(chair);
                    player.startRiding(chair, true);
                }
                
                if (player.isShiftKeyDown() && player.getVehicle() instanceof ArmorStand chair) {
                    if (chair.getPersistentData().getBoolean("ClosetChair")) {
                        chair.discard();
                    }
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
            if (event.getEntity() instanceof Mob mob && mob instanceof Monster) {
                Player closest = mob.level().getNearestPlayer(mob, 16.0D);
                if (closest != null && closest.getPersistentData().getBoolean("IsInCloset")) {
                    mob.getNavigation().stop();
                    mob.setTarget(null);
                    mob.setNoAi(true);
                    mob.getPersistentData().putBoolean("disabled_by_closet", true);
                } else if (mob.getPersistentData().getBoolean("disabled_by_closet")) {
                    mob.setNoAi(false);
                    mob.getPersistentData().remove("disabled_by_closet");
                }
            }
        }
    }
}
