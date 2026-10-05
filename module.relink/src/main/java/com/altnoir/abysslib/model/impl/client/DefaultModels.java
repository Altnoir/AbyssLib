package com.altnoir.abysslib.model.impl.client;

import com.altnoir.abysslib.model.ALAthenaCompat;
import com.altnoir.abysslib.model.AbyssLibReLink;
import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.ALModelAttributes;
import com.altnoir.abysslib.model.api.client.models.ALModelFactory;
import com.altnoir.abysslib.model.api.client.models.FactoryManager;
import com.altnoir.abysslib.model.impl.client.models.*;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * Registers the built-in ReLink model types.
 */
public class DefaultModels {

    /**
     * 模型类型 id 与 JSON 键前缀所用的命名空间。
     */
    public static final String NAMESPACE = AbyssLibReLink.NAMESPACE;

    private static Identifier id(String namespace, String name) {
        return Identifier.fromNamespaceAndPath(namespace, name);
    }

    public static void init() {
        registerAll(NAMESPACE);
        if (ALAthenaCompat.enabled()) {
            registerAll(ALAthenaCompat.ATHENA_NAMESPACE);
        }
    }

    private static void registerAll(String namespace) {
        FactoryManager.register(id(namespace, "ctm"), withAttributes(ConnectedBlockModel.FACTORY));
        FactoryManager.register(id(namespace, "carpet_ctm"), withAttributes(ConnectedCarpetBlockModel.FACTORY));
        FactoryManager.register(id(namespace, "pane_ctm"), withAttributes(PaneConnectedBlockModel.FACTORY));
        FactoryManager.register(id(namespace, "giant"), withAttributes(GiantBlockModel.FACTORY));
        FactoryManager.register(id(namespace, "mural"), withAttributes(GiantBlockModel.FACTORY));
        FactoryManager.register(id(namespace, "pillar"), withAttributes(PillarBlockModel.FACTORY));
        FactoryManager.register(id(namespace, "limited_pillar"), withAttributes(LimitedPillarBlockModel.FACTORY));
        FactoryManager.register(id(namespace, "pane_pillar"), withAttributes(PanePillarBlockModel.FACTORY));
    }

    /**
     * Apply shared tint, render-layer, and emissive attributes to built-in models.
     */
    private static ALModelFactory withAttributes(ALModelFactory factory) {
        return json -> {
            final Supplier<ALBlockModel> delegate = factory.create(json);
            if (!ALModelAttributes.hasAttributes(json)) {
                return delegate;
            }
            final ALModelAttributes attributes = ALModelAttributes.fromJson(json);
            return () -> new AttributeOverrideModel(delegate.get(), attributes);
        };
    }
}
