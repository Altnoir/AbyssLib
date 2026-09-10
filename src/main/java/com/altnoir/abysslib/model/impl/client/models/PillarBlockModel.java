package com.altnoir.abysslib.model.impl.client.models;

import com.google.gson.JsonObject;
import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.ALModelFactory;
import com.altnoir.abysslib.model.api.client.models.ALQuad;
import com.altnoir.abysslib.model.api.client.utils.AppearanceAndTintGetter;
import com.altnoir.abysslib.model.api.client.utils.CtmUtils;
import com.altnoir.abysslib.model.api.client.utils.ALModelUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class PillarBlockModel implements ALBlockModel {

    public static final ALModelFactory FACTORY = new Factory();

    private static final List<ALQuad> CAP = List.of(ALQuad.withSprite(0));

    private final Int2ObjectMap<Material> materials;

    public PillarBlockModel(Int2ObjectMap<Material> materials) {
        this.materials = materials;
    }

    @Override
    public List<ALQuad> getQuads(AppearanceAndTintGetter level, BlockState state, BlockPos pos, Direction direction) {
        BlockPos occludingPos = pos.relative(direction);
        BlockState occludingState = level.getBlockState(occludingPos);
        BlockState appearance = level.getAppearance(state, pos, direction, occludingState, occludingPos);

        if (!appearance.hasProperty(BlockStateProperties.AXIS)) return List.of(ALQuad.withRotation(4, Rotation.NONE));
        Direction.Axis axis = appearance.getValue(BlockStateProperties.AXIS);
        if (axis == direction.getAxis()) {
            return CAP;
        }

        final Rotation rotate = CtmUtils.getPillarRotation(axis, direction);
        final var minMax = ALModelUtils.getMinMax(axis);
        BlockPos posOne = pos.relative(minMax.getFirst());
        BlockPos posTwo = pos.relative(minMax.getSecond());
        BlockState appearanceOne = level.getAppearance(state, pos, direction, level.getBlockState(posOne), posOne);
        BlockState appearanceTwo = level.getAppearance(state, pos, direction, level.getBlockState(posTwo), posTwo);
        final boolean min = !appearanceOne.isAir() && level.getAppearance(posOne, direction, state, pos) == appearanceOne;
        final boolean max = !appearanceTwo.isAir() && level.getAppearance(posTwo, direction, state, pos) == appearanceTwo;

        if (min && max) {
            return List.of(ALQuad.withRotation(2, rotate));
        } else if (min) {
            return List.of(ALQuad.withRotation(3, rotate));
        } else if (max) {
            return List.of(ALQuad.withRotation(1, rotate));
        }
        return List.of(ALQuad.withRotation(4, rotate));
    }

    @Override
    public Map<Direction, List<ALQuad>> getDefaultQuads(Direction direction) {
        Map<Direction, List<ALQuad>> quads = new HashMap<>(Direction.values().length);
        for (Direction dir : Direction.values()) {
            quads.put(dir, List.of(ALQuad.withRotation(4, Rotation.NONE)));
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
            return () -> new PillarBlockModel(materials);
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
