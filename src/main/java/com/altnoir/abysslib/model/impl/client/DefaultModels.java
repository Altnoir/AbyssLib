package com.altnoir.abysslib.model.impl.client;

import com.altnoir.abysslib.AbyssLib;
import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.ALModelAttributes;
import com.altnoir.abysslib.model.api.client.models.ALModelFactory;
import com.altnoir.abysslib.model.api.client.models.FactoryManager;
import com.altnoir.abysslib.model.impl.client.models.*;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * 内置模型的默认注册。
 * <p>
 * 注意：{@link #MODID} 既是模型类型的命名空间（{@code abysslib:ctm} 等），也是模型 JSON 里
 * 声明所用加载器的键名前缀（{@code "abysslib:loader"}）。本库比上游 Athena 改名过命名空间
 * （原为 {@code athena}），因此既有 Athena 格式资源需要按 README 的对照表改写。
 */
public class DefaultModels {

    public static final String MODID = AbyssLib.MOD_ID;

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }

    public static void init() {
        FactoryManager.register(id("ctm"), withAttributes(ConnectedBlockModel.FACTORY));
        FactoryManager.register(id("carpet_ctm"), withAttributes(ConnectedCarpetBlockModel.FACTORY));
        FactoryManager.register(id("pane_ctm"), withAttributes(PaneConnectedBlockModel.FACTORY));
        FactoryManager.register(id("giant"), withAttributes(GiantBlockModel.FACTORY));
        FactoryManager.register(id("mural"), withAttributes(GiantBlockModel.FACTORY));
        FactoryManager.register(id("pillar"), withAttributes(PillarBlockModel.FACTORY));
        FactoryManager.register(id("limited_pillar"), withAttributes(LimitedPillarBlockModel.FACTORY));
        FactoryManager.register(id("pane_pillar"), withAttributes(PanePillarBlockModel.FACTORY));
    }

    /**
     * 让所有内置类型都能识别属性键（{@code tint} / {@code render_type} / 本库扩展的
     * {@code abysslib:emissive}）：上游只有 {@code athena:ctm} 自己解析属性。
     * <p>
     * JSON 里没有任何属性键时原样返回工厂（不包装、零开销）；自定义类型（消费方通过
     * {@link FactoryManager#register} 注册的）不受影响。
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
