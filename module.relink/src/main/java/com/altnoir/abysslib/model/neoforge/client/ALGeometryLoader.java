package com.altnoir.abysslib.model.neoforge.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.altnoir.abysslib.model.api.client.models.neoforge.FactoryManagerImpl;
import com.altnoir.abysslib.model.api.client.utils.ALUnbakedModelLoader;
import com.altnoir.abysslib.model.impl.loading.ALModelDefinitions;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ALGeometryLoader implements IGeometryLoader<ALGeometryLoader.Unbaked> {

    @Override
    public @NotNull Unbaked read(@NotNull JsonObject json, @NotNull JsonDeserializationContext context) throws JsonParseException {
        // 优先 relink:loader；兼容层启用时回退到上游 Athena 的 athena:loader
        String id = ALModelDefinitions.getLoaderDeclaration(json);
        if (id == null || id.isEmpty()) {
            throw new JsonParseException("Missing '" + ALModelDefinitions.LOADER_KEY + "' declaration");
        }
        ResourceLocation loaderId = ResourceLocation.tryParse(id);
        if (loaderId == null) throw new JsonParseException("Invalid loader id: " + id);
        ALUnbakedModelLoader loader = FactoryManagerImpl.get(loaderId);
        if (loader == null) throw new JsonParseException("Unknown loader: " + loaderId);
        return new Unbaked(loader, json);
    }

    public record Unbaked(ALUnbakedModelLoader loader, JsonObject json) implements IUnbakedGeometry<Unbaked> {

        @Override
        public @NotNull BakedModel bake(
                @NotNull IGeometryBakingContext context,
                @NotNull ModelBaker baker,
                @NotNull Function<Material, TextureAtlasSprite> spriteGetter,
                @NotNull ModelState modelState,
                @NotNull ItemOverrides arg3
        ) {
            return loader.loadModel(json).bake(baker, spriteGetter, modelState);
        }
    }
}