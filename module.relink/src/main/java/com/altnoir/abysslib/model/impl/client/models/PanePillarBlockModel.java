package com.altnoir.abysslib.model.impl.client.models;

import com.google.gson.JsonObject;
import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.ALModelFactory;
import com.altnoir.abysslib.model.api.client.models.ALQuad;
import com.altnoir.abysslib.model.api.client.utils.AppearanceAndTintGetter;
import com.altnoir.abysslib.model.api.client.utils.ALModelUtils;
import com.altnoir.abysslib.model.api.client.utils.CtmUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class PanePillarBlockModel implements ALBlockModel {

    public static final ALModelFactory FACTORY = new Factory();

    private static final List<ALQuad> MIDDLE = List.of(new ALQuad(6, 0.4375f, 0.5625f, 1f, 0f, Rotation.NONE, 0.4375f));

    private final Int2ObjectMap<Material> materials;


    public PanePillarBlockModel(Int2ObjectMap<Material> materials) {
        this.materials = materials;
    }

    @Override
    public List<ALQuad> getQuads(AppearanceAndTintGetter level, BlockState state, BlockPos pos, Direction direction) {
        if (direction.getAxis().isVertical()) {
            if (level.getBlockState(pos.relative(direction)) == state) {
                return List.of();
            }
            return getTopQuad(state, direction.getAxisDirection());
        }

        final var rightState = ALModelUtils.getFromDir(state, direction.getCounterClockWise());
        final var leftState = ALModelUtils.getFromDir(state, direction.getClockWise());

        final var upBlockState = level.getAppearance(pos.above(), direction, state, pos);
        final var downBlockState = level.getAppearance(pos.below(), direction, state, pos);

        final var upState = upBlockState.is(state.getBlock()) && ALModelUtils.getFromDir(upBlockState, direction.getCounterClockWise()) && ALModelUtils.getFromDir(upBlockState, direction.getClockWise());
        final var belowState = downBlockState.is(state.getBlock()) && ALModelUtils.getFromDir(downBlockState, direction.getCounterClockWise()) && ALModelUtils.getFromDir(downBlockState, direction.getClockWise());

        int texture = upState && belowState ? 2 : upState ? 3 : belowState ? 1 : 4;

        if (leftState && rightState) {
            final float min = ALModelUtils.getFromDir(state, direction) ? 0.4375f : 0.5f;
            return List.of(
                    new ALQuad(texture, 0, min, 1f, 0.5f, Rotation.NONE, 0.4375f),
                    new ALQuad(texture, 1 - min, 1f, 1f, 0.5f, Rotation.NONE, 0.4375f),
                    new ALQuad(texture, 0, min, 0.5f, 0f, Rotation.NONE, 0.4375f),
                    new ALQuad(texture, 1 - min, 1f, 0.5f, 0f, Rotation.NONE, 0.4375f)
            );
        } else if (leftState) {
            final float min = ALModelUtils.getFromDir(state, direction) ? 0.5625f : 0.4375f;
            return List.of(new ALQuad(0, 0, 1 - min, 1f, 0f, Rotation.NONE, 0.4375f));
        } else if (rightState) {
            final float min = ALModelUtils.getFromDir(state, direction) ? 0.5625f : 0.4375f;
            return List.of(new ALQuad(0, min, 1f, 1f, 0f, Rotation.NONE, 0.4375f));
        } else if (level.getBlockState(pos.relative(direction)).getBlock() != state.getBlock() && !ALModelUtils.getFromDir(state, direction)) {
            return MIDDLE;
        }
        return List.of();
    }

    @Override
    public Int2ObjectMap<TextureAtlasSprite> getTextures(Function<Material, TextureAtlasSprite> getter) {
        final var textures = new Int2ObjectArrayMap<TextureAtlasSprite>();
        for (var entry : this.materials.int2ObjectEntrySet()) {
            textures.put(entry.getIntKey(), getter.apply(entry.getValue()));
        }
        return textures;
    }

    private static final ALQuad TOP_MIDDLE = new ALQuad(5, 0.4375f, 0.5625f, 0.5625f, 0.4375f, Rotation.NONE, 0f, false);
    private static final ALQuad NORTH = new ALQuad(5, 0.4375f, 0.5625f, 1f, 0.5625f, Rotation.NONE, 0f, false);
    private static final ALQuad SOUTH = new ALQuad(5, 0.4375f, 0.5625f, 0.4375f, 0f, Rotation.NONE, 0f, false);
    private static final ALQuad EAST = new ALQuad(5, 0.5625f, 1f, 0.5625f, 0.4375f, Rotation.NONE, 0f, false);
    private static final ALQuad WEST = new ALQuad(5, 0f, 0.4375f, 0.5625f, 0.4375f, Rotation.NONE, 0f, false);

    private List<ALQuad> getTopQuad(BlockState state, Direction.AxisDirection direction) {
        boolean north = ALModelUtils.getFromDir(state, Direction.NORTH);
        boolean south = ALModelUtils.getFromDir(state, Direction.SOUTH);
        boolean east = ALModelUtils.getFromDir(state, Direction.EAST);
        boolean west = ALModelUtils.getFromDir(state, Direction.WEST);
        if (direction == Direction.AxisDirection.NEGATIVE) {
            var tempNorth = north;
            north = south;
            south = tempNorth;
        }


        final List<ALQuad> quads = new ArrayList<>();
        quads.add(TOP_MIDDLE);

        if (north) quads.add(NORTH);
        if (south) quads.add(SOUTH);
        if (east) quads.add(EAST);
        if (west) quads.add(WEST);

        return quads;
    }

    private static class Factory implements ALModelFactory {

        @Override
        public Supplier<ALBlockModel> create(JsonObject json) {
            final var materials = parseMaterials(GsonHelper.getAsJsonObject(json, "ctm_textures"));
            return () -> new PanePillarBlockModel(materials);
        }

        private static Int2ObjectMap<Material> parseMaterials(JsonObject json) {
            Int2ObjectMap<Material> materials = new Int2ObjectArrayMap<>();
            materials.put(0, CtmUtils.blockMat(GsonHelper.getAsString(json, "particle")));
            materials.put(4, CtmUtils.blockMat(GsonHelper.getAsString(json, "self")));

            materials.put(1, CtmUtils.blockMat(GsonHelper.getAsString(json, "top")));
            materials.put(2, CtmUtils.blockMat(GsonHelper.getAsString(json, "center")));
            materials.put(3, CtmUtils.blockMat(GsonHelper.getAsString(json, "bottom")));

            materials.put(5, CtmUtils.blockMat(GsonHelper.getAsString(json, "edge", GsonHelper.getAsString(json, "particle"))));
            materials.put(6, CtmUtils.blockMat(GsonHelper.getAsString(json, "side_edge", GsonHelper.getAsString(json, "particle"))));
            return materials;
        }
    }
}
