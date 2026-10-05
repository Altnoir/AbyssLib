package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// 放宽 jigsaw 深度 codec 和 verifyRange 上限。
@Mixin(value = JigsawStructure.class)
public class JigsawStructureMixin {

    @ModifyArg(
            method = "lambda$static$0",
            at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;"),
            index = 1,
            require = 0)
    private static int abysslib$widenCodecRange(int max) {
        if (max == 20) {
            ALStructureLimits.jigsawRangeApplied = true;
            return ALStructureLimits.JIGSAW_MAX_DEPTH;
        }
        return max;
    }

    @ModifyConstant(method = "verifyRange", constant = @Constant(intValue = 128), require = 0)
    private static int abysslib$widenVerifyRange(int original) {
        ALStructureLimits.jigsawVerifyApplied = true;
        return ALStructureLimits.JIGSAW_MAX_DISTANCE_FROM_CENTER;
    }
}
