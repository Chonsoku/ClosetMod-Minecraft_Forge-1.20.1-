package com.closetfunc.item;

import com.closetfunc.MainCloset;
import com.closetfunc.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MainCloset.MOD_ID);

    public static final RegistryObject<Item> CLOSET_ITEM = ITEMS.register("closet",
            () -> new BlockItem(ModBlocks.CLOSET_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> CLOSET_BATIM_ITEM = ITEMS.register("closet_batim",
            () -> new BlockItem(ModBlocks.CLOSET_BATIM_BLOCK.get(), new Item.Properties()));
    
    public static final RegistryObject<Item> CLOSET_BALDI_ITEM = ITEMS.register("closet_baldi", 
        () -> new BlockItem(ModBlocks.CLOSET_BALDI_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> TYPEWRITER_ITEM = ITEMS.register("typewriter", 
            () -> new BlockItem(ModBlocks.TYPEWRITER_BLOCK.get(), new Item.Properties()));


    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CLOSET_ITEM.get());
            event.accept(TYPEWRITER_ITEM.get());
        }
    }
}
