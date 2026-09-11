package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 放宽原版 jigsaw 结构的两个硬上限（1.21.1）：
 *
 * <pre>
 * // JigsawStructure 的 CODEC 里（静态 lambda 内）
 * Codec.intRange(0, 20)  .fieldOf("size")                     // jigsaw 层深       原版 0..20
 * Codec.intRange(1, 128) .fieldOf("max_distance_from_center") // 结构中心最大半径  原版 1..128
 *
 * // verifyRange(...)
 * structure.maxDistanceFromCenter + i &gt; 128 ? DataResult.error(...) : success
 * </pre>
 *
 * <p><b>两个都要放宽</b>：只放大 {@code max_distance_from_center} 而层深仍是 20，
 * 拼装会在 20 层先到顶，"范围扩大"基本看不出效果 —— 这是最容易误判"改了没生效"的地方。
 * 层深上限 128 是刻意对齐 Integrated API 的 {@code generic_structure}（它就是 {@code intRange(0, 128)}）。
 *
 * <p>实现说明：这两个 {@code intRange} 调用位于 {@code JigsawStructure} 的静态初始化 lambda 里，
 * 该类的静态 lambda 有 11 个（{@code lambda$static$0} … {@code lambda$static$10}），
 * 光看源码无法确定哪个装着目标调用（Integrated API 用"多目标名 + require = 0"规避）。
 * 我们的做法是<b>列全部候选 + 按参数值守卫</b>：只有 max 恰好等于 20 / 128 时才替换，
 * 因此不会误伤其它 {@code intRange}（如 {@code intRange(1, 100)}）。
 */
@Mixin(value = JigsawStructure.class)
public class JigsawStructureMixin {

    /**
     * 放宽 codec 的取值上限。
     * <p>{@code index = 1} 对应 {@code Codec.intRange(min, max)} 的 {@code max} 参数。
     */
    @ModifyArg(
            method = {
                    "lambda$static$0", "lambda$static$1", "lambda$static$2", "lambda$static$3",
                    "lambda$static$4", "lambda$static$5", "lambda$static$6", "lambda$static$7",
                    "lambda$static$8", "lambda$static$9", "lambda$static$10"
            },
            at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;"),
            index = 1,
            require = 0)
    private static int abysslib$widenCodecRange(int max) {
        if (max == 128) {
            ALStructureLimits.jigsawRangeApplied = true;
            return ALStructureLimits.JIGSAW_MAX_DISTANCE_FROM_CENTER;
        }
        if (max == 20) {
            ALStructureLimits.jigsawRangeApplied = true;
            return ALStructureLimits.JIGSAW_MAX_DEPTH;
        }
        return max;
    }

    /**
     * 放宽 {@code verifyRange} 的总量校验：{@code maxDistanceFromCenter + 地形适配补偿 > 128} 会直接拒绝加载，
     * 所以光改 codec 不够。
     * <p>报错文案里的 "must not exceed 128" 是字符串常量，此处不动（只是措辞过时，不影响功能）。
     */
    @ModifyConstant(method = "verifyRange", constant = @Constant(intValue = 128), require = 0)
    private static int abysslib$widenVerifyRange(int original) {
        ALStructureLimits.jigsawVerifyApplied = true;
        return ALStructureLimits.JIGSAW_MAX_DISTANCE_FROM_CENTER;
    }
}
