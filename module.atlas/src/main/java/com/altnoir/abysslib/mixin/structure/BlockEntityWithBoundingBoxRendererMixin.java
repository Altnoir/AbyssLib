package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.client.renderer.blockentity.BlockEntityWithBoundingBoxRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = BlockEntityWithBoundingBoxRenderer.class)
public class BlockEntityWithBoundingBoxRendererMixin {

    @ModifyConstant(method = "getViewDistance", constant = @Constant(intValue = 96), require = 0)
    private int abysslib$widenViewDistance(int original) {
        return Math.max(original, ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE);
    }
}
