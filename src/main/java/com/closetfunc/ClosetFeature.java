package com.closetfunc;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.state.BlockState;

public class ClosetFeature extends Feature<NoneFeatureConfiguration> {
    
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
        BlockState lowerState = MainCloset.CLOSET_BLOCK.get().defaultBlockState()
                .setValue(MainCloset.ClosetBlock.FACING, randomFacing)
                .setValue(MainCloset.ClosetBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(MainCloset.ClosetBlock.OPEN, false);

        BlockState upperState = MainCloset.CLOSET_BLOCK.get().defaultBlockState()
                .setValue(MainCloset.ClosetBlock.FACING, randomFacing)
                .setValue(MainCloset.ClosetBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(MainCloset.ClosetBlock.OPEN, false);
        level.setBlock(originPos, lowerState, 3);
        level.setBlock(topPos, upperState, 3);

        return true;
    }
}
