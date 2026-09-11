package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 结构方块名字输入框长度上限 128 → {@link ALStructureLimits#STRUCTURE_BLOCK_NAME_LENGTH}（256）。
 *
 * <p>原版 1.21.1 在 {@code StructureBlockEditScreen#init} 里：
 * <pre>
 * this.nameEdit.setMaxLength(128);   // ← 第 1 次 setMaxLength（ordinal = 0）
 * ...
 * this.dataEdit.setMaxLength(128);   // ← 第 3 次，是"自定义数据"字段，不能一起改
 * </pre>
 * 所以这里用 {@code ordinal = 0} 只命中 name 那一处，<b>不要</b>用
 * {@code @ModifyConstant(intValue = 128)} 一把梭 —— 那会把 data 字段也放大。
 *
 * <p>服务端没有名字长度限制（{@code StructureBlockEntity#setStructureName} 只做
 * {@code ResourceLocation.tryParse}，包内 {@code readUtf()} 上限 32767），因此只需改客户端 UI。
 */
@Mixin(value = StructureBlockEditScreen.class)
public class StructureBlockEditScreenMixin {

    @ModifyArg(
            method = "init",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setMaxLength(I)V", ordinal = 0),
            index = 0,
            require = 0)
    private int abysslib$widenStructureNameLength(int original) {
        return ALStructureLimits.STRUCTURE_BLOCK_NAME_LENGTH;
    }
}
