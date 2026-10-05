package com.altnoir.abysslib.model.api.client.models;

import com.altnoir.abysslib.model.api.client.utils.AppearanceAndTintGetter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Supplies block quads from the current world context.
 */
public interface ALBlockModel {

    List<ALQuad> getQuads(AppearanceAndTintGetter level, BlockState state, BlockPos pos, Direction direction);

    /**
     * Fallback quads grouped by face.
     */
    default Map<Direction, List<ALQuad>> getDefaultQuads(@Nullable Direction direction) {
        return Map.of();
    }

    Int2ObjectMap<TextureAtlasSprite> getTextures(Function<Material, TextureAtlasSprite> getter);

    default ALModelAttributes getAttributes() {
        return ALModelAttributes.EMPTY;
    }
}
