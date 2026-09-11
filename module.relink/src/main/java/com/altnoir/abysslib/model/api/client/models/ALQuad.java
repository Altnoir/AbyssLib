package com.altnoir.abysslib.model.api.client.models;

import com.altnoir.abysslib.model.api.client.utils.CtmUtils;
import com.altnoir.abysslib.model.impl.client.models.ctm.ConnectedTextureMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

public record ALQuad(int sprite, float left, float right, float top, float bottom, Rotation rotation, float depth, boolean cull) {

    public ALQuad(int sprite, float left, float right, float top, float bottom, Rotation rotation, float depth) {
        this(sprite, left, right, top, bottom, rotation, depth, depth == 0);
    }

    public static ALQuad withSprite(int sprite) {
        return withRotation(sprite, Rotation.NONE);
    }

    public static ALQuad withRotation(int sprite, Rotation rotation) {
        return new ALQuad(sprite, 0, 1, 1, 0, rotation, 0f);
    }

    public static ALQuad withState(ConnectedTextureMap map, Direction direction, boolean first, boolean second, boolean firstSecond, float left, float right, float top, float bottom) {
        final int texture = map.getTexture(direction, CtmUtils.getTexture(first, second, firstSecond));
        return new ALQuad(texture, left, right, top, bottom, Rotation.NONE, 0f);
    }

    public static ALQuad withState(boolean first, boolean second, boolean firstSecond, float left, float right, float top, float bottom) {
        return new ALQuad(CtmUtils.getTexture(first, second, firstSecond), left, right, top, bottom, Rotation.NONE, 0f);
    }

    public static ALQuad withState(boolean first, boolean second, boolean firstSecond, float left, float right, float top, float bottom, float depth) {
        return new ALQuad(CtmUtils.getTexture(first, second, firstSecond), left, right, top, bottom, Rotation.NONE, depth);
    }
}
