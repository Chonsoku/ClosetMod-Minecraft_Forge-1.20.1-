package com.closetfunc.sound;

import com.closetfunc.MainCloset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MainCloset.MOD_ID);

    public static final RegistryObject<SoundEvent> BATIM_SPAWN = SOUNDS.register("batim_spawn",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MainCloset.MOD_ID, "batim_spawn")));

    public static final RegistryObject<SoundEvent> BATIM_OPEN = SOUNDS.register("batim_open",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MainCloset.MOD_ID, "batim_open")));

    public static final RegistryObject<SoundEvent> BATIM_CLOSE = SOUNDS.register("batim_close",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MainCloset.MOD_ID, "batim_close")));

    public static final RegistryObject<SoundEvent> BALDI_SPAWN = SOUNDS.register("baldi_spawn",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MainCloset.MOD_ID, "baldi_spawn")));

    public static final RegistryObject<SoundEvent> BALDI_OPEN = SOUNDS.register("baldi_open",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MainCloset.MOD_ID, "baldi_open")));

    public static final RegistryObject<SoundEvent> BALDI_CLOSE = SOUNDS.register("baldi_close",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MainCloset.MOD_ID, "baldi_close")));
}
