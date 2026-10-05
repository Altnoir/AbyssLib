package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = JigsawStructure.MaxDistance.class)
public class JigsawMaxDistanceMixin {

    @ModifyArg(
            method = "<clinit>",
            at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;"),
            index = 1,
            require = 0)
    private static int abysslib$widenHorizontalRange(int max) {
        if (max == 128) {
            ALStructureLimits.jigsawRangeApplied = true;
            return ALStructureLimits.JIGSAW_MAX_DISTANCE_FROM_CENTER;
        }
        return max;
    }
}
