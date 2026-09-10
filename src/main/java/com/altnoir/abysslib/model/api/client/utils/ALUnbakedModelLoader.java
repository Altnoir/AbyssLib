package com.altnoir.abysslib.model.api.client.utils;

import com.google.gson.JsonObject;
import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.ALModelFactory;
import com.altnoir.abysslib.model.api.client.models.NotNullUnbakedModel;
import com.altnoir.abysslib.model.impl.loading.ALModelDefinitions;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class ALUnbakedModelLoader {

    private final ResourceLocation id;
    private final ALModelFactory factory;
    private final Function<Supplier<ALBlockModel>, NotNullUnbakedModel> loader;

    public ALUnbakedModelLoader(ResourceLocation id, ALModelFactory factory, Function<Supplier<ALBlockModel>, NotNullUnbakedModel> loader) {
        this.id = id;
        this.factory = factory;
        this.loader = loader;
    }

    public @Nullable NotNullUnbakedModel loadModel(ModelResourceLocation modelId) {
        if (modelId == null || "inventory".equals(modelId.getVariant())) return null;
        JsonObject json = ALModelDefinitions.getData(this.id, modelId.id());
        return this.loadModel(json);
    }

    public NotNullUnbakedModel loadModel(JsonObject json) {
        if (json != null) {
            return this.loader.apply(this.factory.create(json));
        }
        return null;
    }
}
