package com.altnoir.abysslib.model.api.client.neoforge;

import com.altnoir.abysslib.model.api.client.utils.AppearanceAndTintGetter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public record WrappedGetter(BlockAndTintGetter getter) implements AppearanceAndTintGetter {
    @Override
    public BlockState getBlockState(BlockPos pos) {
        return getter.getBlockState(pos);
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockPos pos, Direction direction, BlockState fromState, BlockPos fromPos) {
        return state.getAppearance(getter, pos, direction, fromState, fromPos);
    }
}
