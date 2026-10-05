package com.altnoir.abysslib.model.api.client.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface AppearanceAndTintGetter {
    BlockState getBlockState(BlockPos pos);

    BlockState getAppearance(BlockState state, BlockPos pos, Direction direction, BlockState fromState, BlockPos fromPos);

    default BlockState getAppearance(BlockPos pos, Direction direction) {
        BlockState state = getBlockState(pos);
        return getAppearance(state, pos, direction, null, null);
    }

    default BlockState getAppearance(BlockPos pos, Direction direction, BlockState fromState, BlockPos fromPos) {
        BlockState state = getBlockState(pos);
        return getAppearance(state, pos, direction, fromState, fromPos);
    }

    default Query query(BlockPos pos, Direction direction, BlockState fromState, BlockPos fromPos) {
        BlockState state = getBlockState(pos);
        return new Query(state, getAppearance(state, pos, direction, fromState, fromPos));
    }

    record Query(BlockState state, BlockState appearance) {
    }
}
