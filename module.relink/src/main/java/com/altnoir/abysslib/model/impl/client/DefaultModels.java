package com.altnoir.abysslib.model.impl.client;

import com.altnoir.abysslib.model.ALAthenaCompat;
import com.altnoir.abysslib.model.AbyssLibReLink;
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
 * {@link #NAMESPACE} 既是模型类型的命名空间（{@code relink:ctm} 等），也是模型 JSON 里
 * 声明所用加载器的键名前缀（{@code "relink:loader"}）。它<b>不是 modid</b>——
 * modid 是 {@code abysslib_relink}，只用于模组加载与依赖声明。
 * <p>
 * 上游 Athena 用的是 {@code athena}，见 {@link ALAthenaCompat}：兼容层启用时会把同样的
 * 8 个类型再以 {@code athena:} 注册一份，让既有 Athena 资源直接可用。
 */
public class DefaultModels {

    /** 模型类型 id 与 JSON 键前缀所用的命名空间。 */
    public static final String NAMESPACE = AbyssLibReLink.NAMESPACE;

    private static ResourceLocation id(String namespace, String name) {
        return ResourceLocation.fromNamespaceAndPath(namespace, name);
    }

    public static void init() {
        registerAll(NAMESPACE);
        // 兼容层：把同样的类型再以上游 Athena 的命名空间注册一份。
        // 上游 Athena 在场时整体跳过（否则会和上游抢注册同一个几何加载器 id）。
        if (ALAthenaCompat.enabled()) {
            registerAll(ALAthenaCompat.ATHENA_NAMESPACE);
        }
    }

    /** 按给定命名空间注册全部内置类型（上游 Athena 的 8 个类型与本库完全一致）。 */
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
     * 让所有内置类型都能识别属性键（{@code tint} / {@code render_type} / 本库扩展的
     * {@code relink:emissive}）：上游只有 {@code athena:ctm} 自己解析属性。
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
