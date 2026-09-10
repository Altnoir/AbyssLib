package com.altnoir.abysslib.model.impl.client.models;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * 「满亮 quad」的公共实现：本库两个发光功能（整模型发光 {@link EmissiveWrappedModel}、
 * OptiFine 式叠加层 {@link EmissiveOverlayModel}）共用同一套做法，避免各写一份。
 * <p>
 * 做法：**复制**顶点数组（绝不原地改写，原版模型实例常被多个方块/状态共享），必要时把 UV 从原贴图的
 * 图集区间重映射到目标贴图区间，然后以 {@code shade=false} / {@code ambientOcclusion=false} 重建，
 * 最后用 NeoForge 官方的 {@link QuadTransformers#settingMaxEmissivity()} 写满光照。
 * <p>
 * <b>UV 必须重映射</b>的原因：烘焙好的 quad 里 UV 已经是**图集绝对坐标**
 * （{@code FaceBakery.bakeVertex} 写的是 {@code sprite.getU(uv / 16)}），渲染端
 * {@code VertexConsumer.putBulkData} 直接使用这些 UV、不再按 sprite 映射；只换
 * {@code BakedQuad#getSprite()} 会让 quad 采样到**原贴图**的区域。
 */
final class EmissiveQuads {

    private static final IQuadTransformer MAX_EMISSIVE = QuadTransformers.settingMaxEmissivity();

    private EmissiveQuads() {
    }

    /**
     * 把一组 quad 转成满亮副本。
     *
     * @param mapper 每个 quad 的目标贴图；返回 {@code null} 表示跳过该 quad（叠加层用它表达"这张贴图没有 {@code _e} 同族图"）。
     *               返回与 {@code quad.getSprite()} 同一个实例时不重映射 UV。
     * @return 新的列表；没有任何 quad 被转换时返回空列表（调用方据此决定是否复用原列表）
     */
    static List<BakedQuad> fullbright(List<BakedQuad> quads, Function<BakedQuad, TextureAtlasSprite> mapper) {
        if (quads.isEmpty()) {
            return quads;
        }
        List<BakedQuad> out = null;
        for (BakedQuad quad : quads) {
            final TextureAtlasSprite sprite = mapper.apply(quad);
            if (sprite == null) {
                continue;
            }
            if (out == null) {
                out = new ArrayList<>(quads.size());
            }
            out.add(fullbright(quad, sprite));
        }
        return out == null ? List.of() : out;
    }

    private static BakedQuad fullbright(BakedQuad quad, TextureAtlasSprite sprite) {
        final int[] source = quad.getVertices();
        final int[] vertices = Arrays.copyOf(source, source.length);
        if (sprite != quad.getSprite()) {
            remapUvs(vertices, quad.getSprite(), sprite);
        }
        final BakedQuad copy = new BakedQuad(
                vertices,
                quad.getTintIndex(),
                quad.getDirection(),
                sprite,
                false, // shade：去掉方向性明暗
                false  // ambientOcclusion：交给 FlatQuadLighter
        );
        MAX_EMISSIVE.processInPlace(copy);
        return copy;
    }

    /** 把顶点 UV 从基贴图在图集里的区间，线性映射到目标贴图的区间。 */
    private static void remapUvs(int[] vertices, TextureAtlasSprite from, TextureAtlasSprite to) {
        final float u0 = from.getU0();
        final float u1 = from.getU1();
        final float v0 = from.getV0();
        final float v1 = from.getV1();
        final float scaleU = (u1 - u0) == 0.0F ? 0.0F : (to.getU1() - to.getU0()) / (u1 - u0);
        final float scaleV = (v1 - v0) == 0.0F ? 0.0F : (to.getV1() - to.getV0()) / (v1 - v0);
        final float offsetU = to.getU0() - u0 * scaleU;
        final float offsetV = to.getV0() - v0 * scaleV;
        for (int i = 0; i < 4; i++) {
            final int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            vertices[offset] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[offset]) * scaleU + offsetU);
            vertices[offset + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[offset + 1]) * scaleV + offsetV);
        }
    }
}
