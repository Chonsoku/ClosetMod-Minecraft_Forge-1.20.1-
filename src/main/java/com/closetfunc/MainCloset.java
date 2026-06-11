package com.closetfunc;

import com.closetfunc.block.ModBlocks;
import com.closetfunc.block_entity.ModBlockEntities;
import com.closetfunc.item.ModItems;
import com.closetfunc.sound.ModSounds;
import com.closetfunc.worldgen.ModFeature;
import com.closetfunc.event.BadRewards;
import com.closetfunc.event.GoodRewards;
import com.closetfunc.event.ModEvents;
import com.closetfunc.client.ClosetClient;
import com.closetfunc.network.ModMessages;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MainCloset.MOD_ID)
public class MainCloset {
    public static final String MOD_ID = "closet_mod";

    public MainCloset() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрируем все компоненты мода из новых пакетов
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModFeature.FEATURES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModMessages.register();

        MinecraftForge.EVENT_BUS.register(ModEvents.class);
        MinecraftForge.EVENT_BUS.register(GoodRewards.class);
        MinecraftForge.EVENT_BUS.register(BadRewards.class);
        MinecraftForge.EVENT_BUS.register(ModBlockEntities.ClosetBlockEntity.class);
        
        // Пасхалка на "Death Note"
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent event) -> {
            event.getDispatcher().register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("closetmod_trigger_heartattack")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {
                    net.minecraft.server.level.ServerPlayer serverPlayer = context.getSource().getPlayer();
                    if (serverPlayer != null && !serverPlayer.getPersistentData().contains("DeathNoteTimeTarget")) {
                        // Смерть через 1200 тиков (60 секунд)
                        long timeOfDeath = serverPlayer.level().getGameTime() + 1200L;
                        serverPlayer.getPersistentData().putLong("DeathNoteTimeTarget", timeOfDeath);
                    }
                    return 1;
                })
            );
        });


        if (FMLEnvironment.dist.isClient()) {
            ClosetClient.init();
        }

        bus.addListener(ModItems::addCreative);
    }
}
