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

public class GiantBlockModel implements ALBlockModel {

    public static final ALModelFactory FACTORY = new Factory();

    private final Int2ObjectMap<Material> materials;
    private final int width;
    private final int height;

    public GiantBlockModel(Int2ObjectMap<Material> materials, int width, int height) {
        this.materials = materials;
        this.width = width;
        this.height = height;
    }

    @Override
    public List<ALQuad> getQuads(AppearanceAndTintGetter level, BlockState blockState, BlockPos pos, Direction direction) {
        int x = Math.abs(pos.getX());
        int y = Math.abs(pos.getY());
        int z = Math.abs(pos.getZ());

        return switch (direction.getAxis()) {
            case X -> {
                if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    z = Math.abs(z - width - 1);
                }
                yield List.of(ALQuad.withSprite(1 + (z % width) + (y % height) * height));
            }
            case Z -> {
                if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                    x = Math.abs(x - width - 1);
                }
                yield List.of(ALQuad.withSprite(1 + (x % width) + (y % height) * height));
            }
            default -> {
                if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                    z = Math.abs(z - width - 1);
                }
                yield List.of(ALQuad.withSprite(1 + (x % width) + (z % height) * height));
            }
        };
    }

    @Override
    public Map<Direction, List<ALQuad>> getDefaultQuads(Direction direction) {
        Map<Direction, List<ALQuad>> quads = new HashMap<>(Direction.values().length);
        for (Direction dir : Direction.values()) {
            quads.put(dir, List.of(ALQuad.withSprite(0)));
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
            final int width = GsonHelper.getAsInt(json, "width");
            final int height = GsonHelper.getAsInt(json, "height");
            final var materials = parseMaterials(GsonHelper.getAsJsonObject(json, "ctm_textures"), width, height);
            return () -> new GiantBlockModel(materials, width, height);
        }

        private static Int2ObjectMap<Material> parseMaterials(JsonObject json, int width, int height) {
            Int2ObjectMap<Material> materials = new Int2ObjectArrayMap<>();
            materials.put(0, CtmUtils.blockMat(GsonHelper.getAsString(json, "particle")));

            for (int i = 1; i <= width * height; i++) {
                final var material = CtmUtils.blockMat(GsonHelper.getAsString(json, String.valueOf(i)));
                materials.put(i, material);
            }

            return materials;
        }
    }
}
