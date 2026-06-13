package com.closetfunc.worldgen;

import com.closetfunc.MainCloset;
import com.closetfunc.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


@SuppressWarnings("null")
public class ModFeature {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, MainCloset.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CLOSET_FEATURE =
            FEATURES.register("closet_feature", () -> new ClosetFeature(NoneFeatureConfiguration.CODEC));

    public static class ClosetFeature extends Feature<NoneFeatureConfiguration> {
        public ClosetFeature(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos originPos = context.origin();
            RandomSource random = context.random();
            BlockPos floorPos = originPos.below();

            if (!level.getBlockState(floorPos).isFaceSturdy(level, floorPos, Direction.UP)) {
                return false;
            }

            BlockPos topPos = originPos.above();
            if (!level.getBlockState(originPos).canBeReplaced() || !level.getBlockState(topPos).canBeReplaced()) {
                return false;
            }

            Direction randomFacing = Direction.Plane.HORIZONTAL.getRandomDirection(random);

            BlockState lowerState = ModBlocks.CLOSET_BLOCK.get().defaultBlockState()
                    .setValue(ModBlocks.ClosetBlock.FACING, randomFacing)
                    .setValue(ModBlocks.ClosetBlock.HALF, DoubleBlockHalf.LOWER)
                    .setValue(ModBlocks.ClosetBlock.OPEN, false);

            BlockState upperState = ModBlocks.CLOSET_BLOCK.get().defaultBlockState()
                    .setValue(ModBlocks.ClosetBlock.FACING, randomFacing)
                    .setValue(ModBlocks.ClosetBlock.HALF, DoubleBlockHalf.UPPER)
                    .setValue(ModBlocks.ClosetBlock.OPEN, false);

            level.setBlock(originPos, lowerState, 3);
            level.setBlock(topPos, upperState, 3);

            return true;
        }
    }
}
