package com.altnoir.abysslib.model.api.client.neoforge;

import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.ALModelAttributes;
import com.altnoir.abysslib.model.api.client.models.ALQuad;
import com.altnoir.abysslib.model.api.client.utils.ALModelUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ALBlockStateModel implements DynamicBlockStateModel {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ALBlockModel model;
    private final Int2ObjectMap<TextureAtlasSprite> textures;
    private final ALModelAttributes attributes;
    private final Material.Baked particleMaterial;
    private final int materialFlags;

    public ALBlockStateModel(ALBlockModel model, Function<Material, TextureAtlasSprite> spriteGetter) {
        this.model = model;
        this.textures = model.getTextures(spriteGetter);
        this.attributes = model.getAttributes();
        int flags = 0;
        for (TextureAtlasSprite sprite : this.textures.values()) {
            if (this.attributes != null && this.attributes.layer() != null
                    ? this.attributes.layer().translucent()
                    : sprite.transparency().hasTranslucent()) {
                flags |= BakedQuad.FLAG_TRANSLUCENT;
            }
            if (sprite.contents().isAnimated()) flags |= BakedQuad.FLAG_ANIMATED;
        }
        this.materialFlags = flags;
        TextureAtlasSprite particle = this.textures.get(0);
        if (particle == null) {
            particle = this.textures.values().stream().findFirst()
                    .orElseGet(() -> spriteGetter.apply(new Material(MissingTextureAtlasSprite.getLocation())));
        }
        this.particleMaterial = new Material.Baked(particle, false);
    }

    @Override
    @Deprecated
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), random, parts);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        try {
            WrappedGetter getter = new WrappedGetter(level);
            Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
            Map<Direction, List<BakedQuad>> unculled = new EnumMap<>(Direction.class);
            for (Direction direction : DIRECTIONS) {
                for (ALQuad quad : this.model.getQuads(getter, state, pos, direction)) {
                    TextureAtlasSprite sprite = this.textures.get(quad.sprite());
                    if (sprite == null) continue;
                    List<BakedQuad> baked = ALQuadBaker.bakeQuad(quad, direction, sprite, this.attributes);
                    (quad.cull() ? culled : unculled).computeIfAbsent(direction, ignored -> new ArrayList<>()).addAll(baked);
                }
            }
            if (!culled.isEmpty() || !unculled.isEmpty()) {
                parts.add(new Part(culled, unculled, this.particleMaterial, this.attributes != null && this.attributes.emissive()));
            }
        } catch (Exception e) {
            ALModelUtils.LOGGER.error("Failed to collect AbyssLib block model quads", e);
            throw e;
        }
    }

    @Override
    @Deprecated
    public Material.Baked particleMaterial() {
        return this.particleMaterial;
    }

    @Override
    @Deprecated
    public int materialFlags() {
        return this.materialFlags;
    }

    private record Part(
            Map<Direction, List<BakedQuad>> culled,
            Map<Direction, List<BakedQuad>> unculled,
            Material.Baked particleMaterial,
            boolean emissive
    ) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? flatten(this.unculled) : this.culled.getOrDefault(direction, List.of());
        }

        @Override
        @Deprecated
        public boolean useAmbientOcclusion() {
            return !this.emissive;
        }

        @Override
        public int materialFlags() {
            int flags = 0;
            for (List<BakedQuad> quads : this.culled.values()) {
                for (BakedQuad quad : quads) flags |= quad.materialInfo().flags();
            }
            for (List<BakedQuad> quads : this.unculled.values()) {
                for (BakedQuad quad : quads) flags |= quad.materialInfo().flags();
            }
            return flags;
        }

        private static List<BakedQuad> flatten(Map<Direction, List<BakedQuad>> quads) {
            List<BakedQuad> result = new ArrayList<>();
            quads.values().forEach(result::addAll);
            return result;
        }
    }
}
