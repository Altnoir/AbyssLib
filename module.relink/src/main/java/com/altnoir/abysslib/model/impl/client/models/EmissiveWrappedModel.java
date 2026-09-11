package com.altnoir.abysslib.model.impl.client.models;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 「包裹原版模型发光」：**本库扩展**。
 * <p>
 * 用途：blockstate 根上只写 {@code "relink:emissive": true}（没有 {@code relink:loader}）时，
 * 不替换方块模型——保留 {@code variants} 指向的原版模型（形状、贴图、连接关系全不动），
 * 只把它的每个 quad 换成满亮副本，于是暗处看起来就是发光。
 * <p>
 * 具体转换（复制顶点、关 shade/AO、写满光照）统一在 {@link EmissiveQuads} 里实现，
 * 与 OptiFine 式叠加层共用；物品栏变体（{@code inventory}）由调用方跳过。
 */
public class EmissiveWrappedModel extends BakedModelWrapper<BakedModel> {

    public EmissiveWrappedModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        return fullbright(originalModel.getQuads(state, side, rand, data, renderType));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return fullbright(originalModel.getQuads(state, side, rand));
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        return TriState.FALSE;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return originalModel.getModelData(level, pos, state, modelData);
    }

    /** 保留原贴图（{@code mapper} 返回自身 → 不重映射 UV），整体转成满亮。 */
    private static List<BakedQuad> fullbright(List<BakedQuad> quads) {
        if (quads.isEmpty()) {
            return quads;
        }
        final List<BakedQuad> converted = EmissiveQuads.fullbright(quads, BakedQuad::getSprite);
        return converted.isEmpty() ? quads : converted;
    }
}
