package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// 命令路径不经过结构 codec，因此需要单独放宽。
@Mixin(value = JigsawPlacement.class)
public class JigsawPlacementMixin {

    @ModifyConstant(method = "generateJigsaw", constant = @Constant(intValue = 128), require = 0)
    private static int abysslib$widenPlaceCommandRange(int original) {
        ALStructureLimits.placeJigsawApplied = true;
        return ALStructureLimits.JIGSAW_MAX_DISTANCE_FROM_CENTER;
    }
}
