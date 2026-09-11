package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * {@code /place jigsaw} 命令路径的距离上限。
 *
 * <p>原版 {@code JigsawPlacement#generateJigsaw} 直接给 {@code addPieces(...)} 传了硬编码的 {@code 128}，
 * 它<b>不经过</b> {@code JigsawStructure.CODEC}，所以只改 codec 的话，命令放置的结构仍然卡在 128。
 * 这一处就是用来补齐命令路径的。
 */
@Mixin(value = JigsawPlacement.class)
public class JigsawPlacementMixin {

    @ModifyConstant(method = "generateJigsaw", constant = @Constant(intValue = 128), require = 0)
    private static int abysslib$widenPlaceCommandRange(int original) {
        ALStructureLimits.placeJigsawApplied = true;
        return ALStructureLimits.JIGSAW_MAX_DISTANCE_FROM_CENTER;
    }
}
