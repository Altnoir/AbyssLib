package com.altnoir.abysslib.model.api.client.neoforge;

import com.altnoir.abysslib.model.api.client.models.ALModelAttributes;
import com.altnoir.abysslib.model.api.client.models.ALQuad;
import com.altnoir.abysslib.model.api.client.models.TintProvider;
import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import net.neoforged.neoforge.client.model.quad.UVTransform;

import java.util.List;

public final class ALQuadBaker {
    private ALQuadBaker() {
    }

    public static List<BakedQuad> bakeQuad(ALQuad quad, Direction direction, TextureAtlasSprite sprite, ALModelAttributes attributes) {
        boolean emissive = attributes != null && attributes.emissive();
        MutableQuad baked = new MutableQuad()
                .setCubeFaceFromSpriteCoords(direction, quad.left(), quad.bottom(), quad.right(), quad.top(), quad.depth())
                .setSprite(new Material.Baked(sprite, false))
                .bakeUvsFromPosition(UVTransform.of(Quadrant.values()[quad.rotation().ordinal()], false, false))
                .setShade(!emissive)
                .setLightEmission(emissive ? 15 : 0)
                .setAmbientOcclusion(!emissive);

        TintProvider tint = attributes == null ? null : attributes.tint();
        if (tint instanceof TintProvider.Index(int index1)) {
            baked.setTintIndex(index1);
        } else if (tint instanceof TintProvider.Static(int color1)) {
            baked.setColor(color1);
        }

        ChunkSectionLayer layer = attributes == null ? null : attributes.layer();
        if (layer != null) {
            baked.setSprite(sprite, layer, baked.requiredItemRenderType());
        }

        return List.of(baked.toBakedQuad());
    }
}
