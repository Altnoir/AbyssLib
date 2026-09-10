package com.altnoir.abysslib.model.api.client.neoforge;

import com.altnoir.abysslib.model.api.client.models.ALModelAttributes;
import com.altnoir.abysslib.model.api.client.models.ALQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public class ALQuadBaker {

    public static List<BakedQuad> bakeQuad(ALQuad quad, Direction direction, TextureAtlasSprite sprite, ALModelAttributes attributes) {
        final Vector3f start = getStartPos(quad, direction);
        final Vector3f end = getEndPos(quad, direction);
        final BlockElementFace face = ALBlockElementFace.of(quad, direction, start, end, attributes);
        // 发光面关闭方向性明暗（shade=false），配合 15/15 光照与关闭 AO，贴图颜色即所见
        final boolean shade = attributes == null || !attributes.isEmissive();
        final BlockElement element = new BlockElement(start, end, Map.of(direction.getOpposite(), face), null, shade);
        return UnbakedGeometryHelper.bakeElements(
                List.of(element),
                mat -> sprite,
                BlockModelRotation.X0_Y0
        );
    }

    private static Vector3f getStartPos(ALQuad quad, Direction direction) {
        return switch (direction) {
            case NORTH -> new Vector3f((1 - quad.left()) * 16f, quad.bottom() * 16f, quad.depth() * 16f);
            case SOUTH -> new Vector3f(quad.right() * 16f, quad.bottom() * 16f, (1-quad.depth()) * 16f);
            case WEST -> new Vector3f(quad.depth() * 16f, quad.bottom() * 16f, quad.right() * 16f);
            case EAST -> new Vector3f((1 - quad.depth()) * 16f, quad.bottom() * 16f,  (1 - quad.left()) * 16f);
            case DOWN -> new Vector3f(quad.left() * 16f, quad.depth() * 16f, quad.top() * 16f);
            case UP -> new Vector3f(quad.left() * 16f, (1 - quad.depth()) * 16f, (1 - quad.bottom()) * 16f);
        };
    }

    private static Vector3f getEndPos(ALQuad quad, Direction direction) {
        return switch (direction) {
            case NORTH -> new Vector3f((1 - quad.right()) * 16f, quad.top() * 16f, quad.depth() * 16f);
            case SOUTH -> new Vector3f(quad.left() * 16f, quad.top() * 16f, (1 - quad.depth()) * 16f);
            case WEST -> new Vector3f(quad.depth() * 16f, quad.top() * 16f, quad.left() * 16f);
            case EAST -> new Vector3f((1 - quad.depth()) * 16f, quad.top() * 16f, (1 - quad.right()) * 16f);
            case DOWN -> new Vector3f(quad.right() * 16f, quad.depth() * 16f, quad.bottom() * 16f);
            case UP -> new Vector3f(quad.right() * 16f, quad.depth() * 16f, (1 - quad.top()) * 16f);
        };
    }
}
