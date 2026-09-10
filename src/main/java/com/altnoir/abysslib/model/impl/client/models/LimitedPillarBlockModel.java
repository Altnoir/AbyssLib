package com.altnoir.abysslib.model.impl.client.models;

import com.google.gson.JsonObject;
import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.ALModelFactory;
import com.altnoir.abysslib.model.api.client.models.ALQuad;
import com.altnoir.abysslib.model.api.client.utils.AppearanceAndTintGetter;
import com.altnoir.abysslib.model.api.client.utils.CtmUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class LimitedPillarBlockModel implements ALBlockModel {

    public static final ALModelFactory FACTORY = new Factory();

    private static final List<ALQuad> CENTER = List.of(ALQuad.withSprite(2));
    private static final List<ALQuad> TOP = List.of(ALQuad.withSprite(1));
    private static final List<ALQuad> BOTTOM = List.of(ALQuad.withSprite(3));
    private static final List<ALQuad> SELF = List.of(ALQuad.withSprite(4));
    private static final List<ALQuad> CAP = List.of(ALQuad.withSprite(0));

    private final Int2ObjectMap<Material> materials;

    public LimitedPillarBlockModel(Int2ObjectMap<Material> materials) {
        this.materials = materials;
    }

    @Override
    public List<ALQuad> getQuads(AppearanceAndTintGetter level, BlockState state, BlockPos pos, Direction direction) {
        BlockPos occludingPos = pos.relative(direction);
        BlockState appearance = level.getAppearance(state, pos, direction, level.getBlockState(occludingPos), occludingPos);
        BlockState occludingAppearance = level.getAppearance(occludingPos, direction.getOpposite(), state, pos);
        if (!appearance.isAir() && occludingAppearance.is(appearance.getBlock())) {
            return List.of();
        }

        if (direction.getAxis().isVertical()) {
            return CAP;
        }

        BlockPos posAbove = pos.above();
        BlockPos posBelow = pos.below();
        BlockState appearanceAbove = level.getAppearance(state, pos, direction, level.getBlockState(posAbove), posAbove);
        BlockState appearanceBelow = level.getAppearance(state, pos, direction, level.getBlockState(posBelow), posBelow);
        final boolean min = !appearanceAbove.isAir() && level.getAppearance(posAbove, direction, state, pos).is(appearanceAbove.getBlock());
        final boolean max = !appearanceBelow.isAir() && level.getAppearance(posBelow, direction, state, pos).is(appearanceBelow.getBlock());

        if (min && max) {
            return CENTER;
        } else if (min) {
            return BOTTOM;
        } else if (max) {
            return TOP;
        }
        return SELF;
    }

    @Override
    public Map<Direction, List<ALQuad>> getDefaultQuads(Direction direction) {
        Map<Direction, List<ALQuad>> quads = new HashMap<>(Direction.values().length);
        for (Direction dir : Direction.values()) {
            quads.put(dir, SELF);
        }
        return quads;
    }

    @Override
    public Int2ObjectMap<TextureAtlasSprite> getTextures(Function<Material, TextureAtlasSprite> getter) {
        Int2ObjectMap<TextureAtlasSprite> textures = new Int2ObjectArrayMap<>();
        for (var entry : materials.int2ObjectEntrySet()) {
            textures.put(entry.getIntKey(), getter.apply(entry.getValue()));
        }
        return textures;
    }

    private static class Factory implements ALModelFactory {

        @Override
        public Supplier<ALBlockModel> create(JsonObject json) {
            final var materials = parseMaterials(GsonHelper.getAsJsonObject(json, "ctm_textures"));
            return () -> new LimitedPillarBlockModel(materials);
        }

        private static Int2ObjectMap<Material> parseMaterials(JsonObject json) {
            Int2ObjectMap<Material> materials = new Int2ObjectArrayMap<>();
            materials.put(0, CtmUtils.blockMat(GsonHelper.getAsString(json, "particle")));
            materials.put(4, CtmUtils.blockMat(GsonHelper.getAsString(json, "self")));

            materials.put(1, CtmUtils.blockMat(GsonHelper.getAsString(json, "top")));
            materials.put(2, CtmUtils.blockMat(GsonHelper.getAsString(json, "center")));
            materials.put(3, CtmUtils.blockMat(GsonHelper.getAsString(json, "bottom")));

            return materials;
        }
    }
}
