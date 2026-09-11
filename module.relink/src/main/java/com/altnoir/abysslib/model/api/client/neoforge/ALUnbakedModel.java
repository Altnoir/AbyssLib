package com.altnoir.abysslib.model.api.client.neoforge;

import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.NotNullUnbakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ALUnbakedModel implements NotNullUnbakedModel {

    private final Supplier<ALBlockModel> model;

    public ALUnbakedModel(Supplier<ALBlockModel> model) {
        this.model = model;
    }

    @Override
    public @NotNull Collection<ResourceLocation> getDependencies() {
        return List.of();
    }

    @Override
    public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> function) {

    }

    @NotNull
    @Override
    public BakedModel bake(@NotNull ModelBaker modelBaker, @NotNull Function<Material, TextureAtlasSprite> function, @NotNull ModelState modelState) {
        return bake(function);
    }

    /**
     * 本库扩展：不依赖 {@link ModelBaker} 的烘焙入口。
     * <p>
     * 原因：NeoForge 的 {@code ModelEvent.ModifyBakingResult} 只暴露 {@link net.minecraft.client.resources.model.ModelBakery}
     * （其内部 {@code ModelBakerImpl} 为包私有，无法构造），而本模型的烘焙只用到贴图 getter。
     * 几何加载器路径（{@code ALGeometryLoader}）仍走上面带 {@link ModelBaker} 的标准重载。
     */
    @NotNull
    public BakedModel bake(@NotNull Function<Material, TextureAtlasSprite> spriteGetter) {
        return new ALBakedModel(this.model.get(), spriteGetter);
    }
}
