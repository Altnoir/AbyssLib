package com.altnoir.abysslib.model.impl.client.models;

import com.altnoir.abysslib.client.ALClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EmissiveOverlayModel extends DelegateBlockStateModel {
    private final Map<Identifier, Optional<TextureAtlasSprite>> sprites = new ConcurrentHashMap<>();

    public EmissiveOverlayModel(BlockStateModel delegate) {
        super(delegate);
    }

    @Override
    @Deprecated
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), random, parts);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        List<BlockStateModelPart> original = new ArrayList<>();
        this.delegate.collectParts(level, pos, state, random, original);
        original.forEach(part -> parts.add(new OverlayPart(part, this)));
    }

    private List<BakedQuad> addOverlay(List<BakedQuad> base) {
        if (!ALClientConfig.overlayEnabled() || base.isEmpty()) return base;
        List<BakedQuad> result = null;
        for (BakedQuad quad : base) {
            TextureAtlasSprite sprite = overlaySprite(quad.materialInfo().sprite());
            if (sprite == null) continue;
            if (result == null) result = new ArrayList<>(base);
            result.add(EmissiveQuads.overlay(quad, sprite));
        }
        return result == null ? base : result;
    }

    @Nullable
    private TextureAtlasSprite overlaySprite(TextureAtlasSprite base) {
        String suffix = ALClientConfig.emissiveSuffix;
        if (suffix.isEmpty()) return null;
        Identifier baseId = base.contents().name();
        if (ALClientConfig.isExcluded(baseId)) return null;
        Identifier overlayId = baseId.withSuffix(suffix);
        return this.sprites.computeIfAbsent(overlayId, id -> {
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                    .getAtlasOrThrow(base.atlasLocation()).getSprite(id);
            return sprite != null && !sprite.contents().name().equals(id) ? Optional.of(sprite) : Optional.empty();
        }).orElse(null);
    }

    private record OverlayPart(BlockStateModelPart delegate,
                               EmissiveOverlayModel owner) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return this.owner.addOverlay(this.delegate.getQuads(direction));
        }

        @Override
        @Deprecated
        public boolean useAmbientOcclusion() {
            return this.delegate.useAmbientOcclusion();
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
