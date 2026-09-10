package com.altnoir.abysslib.model.api.client.models;

import com.google.gson.JsonObject;

import java.util.function.Supplier;

public interface ALModelFactory {

    Supplier<ALBlockModel> create(JsonObject json);
}
