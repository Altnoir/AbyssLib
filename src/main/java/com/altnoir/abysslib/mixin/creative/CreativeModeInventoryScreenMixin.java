package com.altnoir.abysslib.mixin.creative;

import com.altnoir.abysslib.client.creative.ALSectionedCreativeTabRenderer;
import com.altnoir.abysslib.creative.ALSectionedCreativeModeTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private float scrollOffs;

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void abysslib$extractSectionHeadings(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (selectedTab instanceof ALSectionedCreativeModeTab sectionedTab) {
            ALSectionedCreativeTabRenderer.extract(
                    (CreativeModeInventoryScreen) (Object) this,
                    graphics,
                    sectionedTab,
                    scrollOffs
            );
        } else {
            ALSectionedCreativeTabRenderer.clearHeadingSlots();
        }
    }
}
