package com.altnoir.abysslib.model.api.client.utils;

import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.ALModelFactory;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public final class ALUnbakedModelLoader {
    private final Identifier id;
    private final ALModelFactory factory;

    public ALUnbakedModelLoader(Identifier id, ALModelFactory factory) {
        this.id = id;
        this.factory = factory;
    }

    public Identifier id() {
        return this.id;
    }

    public ALBlockModel loadModel(JsonObject json) {
        return json == null ? null : this.factory.create(json).get();
    }
}
