package com.altnoir.abysslib.model.impl.client.models;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class EmissiveWrappedModel extends DelegateBlockStateModel {
    public EmissiveWrappedModel(net.minecraft.client.renderer.block.dispatch.BlockStateModel delegate) {
        super(delegate);
    }

    @Override
    @Deprecated
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), random, parts);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        List<BlockStateModelPart> original = new ArrayList<>();
        this.delegate.collectParts(level, pos, state, random, original);
        original.forEach(part -> parts.add(new FullbrightPart(part)));
    }

    private record FullbrightPart(BlockStateModelPart delegate) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return this.delegate.getQuads(direction).stream().map(EmissiveQuads::fullbright).toList();
        }

        @Override
        @Deprecated
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
            return this.delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return this.delegate.materialFlags();
        }
    }
}
