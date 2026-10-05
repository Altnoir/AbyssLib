package com.altnoir.abysslib.model.api.client.models.neoforge;

import com.altnoir.abysslib.model.api.client.models.ALModelFactory;
import com.altnoir.abysslib.model.api.client.utils.ALUnbakedModelLoader;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FactoryManagerImpl {

    private static final Map<Identifier, ALUnbakedModelLoader> FACTORIES = new HashMap<>();

    public static void register(Identifier type, ALModelFactory factory) {
        if (FACTORIES.containsKey(type)) {
            throw new IllegalArgumentException("Factory already registered for type: " + type);
        }
        FACTORIES.put(type, new ALUnbakedModelLoader(type, factory));
    }

    public static ALUnbakedModelLoader get(Identifier type) {
        return FACTORIES.get(type);
    }

    public static Collection<Identifier> getTypes() {
        return FACTORIES.keySet();
    }
}
