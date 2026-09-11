package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.client.renderer.blockentity.StructureBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 结构方块轮廓（showBoundingBox）的可视距离：原版 {@code 96} → 至少等于结构上限（128）。
 *
 * <p>取 {@code max(96, STRUCTURE_BLOCK_MAX_SIZE)} 而不是照抄 Integrated API 的 {@code NEW_STRUCTURE_SIZE / 2}：
 * 它是按 512 算的（256 &gt; 96 才有效）；如果我们直接写 128/2 = 64，反而比原版的 96 <b>更短</b>，
 * 大结构的轮廓会更早消失。
 */
@Mixin(value = StructureBlockRenderer.class)
public class StructureBlockRendererMixin {

    @ModifyConstant(method = "getViewDistance", constant = @Constant(intValue = 96), require = 0)
    private int abysslib$widenViewDistance(int original) {
        return Math.max(original, ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE);
    }
}
