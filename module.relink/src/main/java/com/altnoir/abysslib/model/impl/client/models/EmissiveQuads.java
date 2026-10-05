package com.altnoir.abysslib.model.impl.client.models;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;

final class EmissiveQuads {
    private EmissiveQuads() {
    }

    static BakedQuad fullbright(BakedQuad source) {
        return remap(source, source.materialInfo().sprite(), source.materialInfo().layer(), false);
    }

    static BakedQuad overlay(BakedQuad source, TextureAtlasSprite sprite) {
        ChunkSectionLayer layer = source.materialInfo().layer() == ChunkSectionLayer.SOLID
                ? ChunkSectionLayer.CUTOUT
                : source.materialInfo().layer();
        return remap(source, sprite, layer, true);
    }

    private static BakedQuad remap(BakedQuad source, TextureAtlasSprite sprite, ChunkSectionLayer layer, boolean remapUv) {
        TextureAtlasSprite original = source.materialInfo().sprite();
        long[] uvs = new long[4];
        for (int i = 0; i < 4; i++) {
            long packed = source.packedUV(i);
            float u = UVPair.unpackU(packed);
            float v = UVPair.unpackV(packed);
            if (remapUv && sprite != original) {
                float localU = (u - original.getU0()) / (original.getU1() - original.getU0());
                float localV = (v - original.getV0()) / (original.getV1() - original.getV0());
                u = sprite.getU(localU);
                v = sprite.getV(localV);
            }
            uvs[i] = UVPair.pack(u, v);
        }
        BakedQuad.MaterialInfo old = source.materialInfo();
        BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(
                sprite, layer, old.itemRenderType(), old.tintIndex(), false, 15, false);
        return new BakedQuad(
                source.position0(), source.position1(), source.position2(), source.position3(),
                uvs[0], uvs[1], uvs[2], uvs[3], source.direction(), material,
                source.bakedNormals(), source.bakedColors());
    }
}
