package com.altnoir.abysslib.model.api.client.models.neoforge;

import com.altnoir.abysslib.model.api.client.models.ALModelFactory;
import com.altnoir.abysslib.model.api.client.neoforge.ALUnbakedModel;
import com.altnoir.abysslib.model.api.client.utils.ALUnbakedModelLoader;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FactoryManagerImpl {

    private static final Map<ResourceLocation, ALUnbakedModelLoader> FACTORIES = new HashMap<>();

    public static void register(ResourceLocation type, ALModelFactory factory) {
        if (FACTORIES.containsKey(type)) {
            throw new IllegalArgumentException("Factory already registered for type: " + type);
        }
        FACTORIES.put(type, new ALUnbakedModelLoader(type, factory, ALUnbakedModel::new));
    }

    public static ALUnbakedModelLoader get(ResourceLocation type) {
        return FACTORIES.get(type);
    }

    public static Collection<ResourceLocation> getTypes() {
        return FACTORIES.keySet();
    }
}
