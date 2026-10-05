package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = StructureBlockEditScreen.class)
public class StructureBlockEditScreenMixin {

    // 只修改名称输入框；其他输入框也会调用同一方法。
    @ModifyArg(
            method = "init",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setMaxLength(I)V", ordinal = 0),
            index = 0,
            require = 0)
    private int abysslib$widenStructureNameLength(int original) {
        return ALStructureLimits.STRUCTURE_BLOCK_NAME_LENGTH;
    }
}
