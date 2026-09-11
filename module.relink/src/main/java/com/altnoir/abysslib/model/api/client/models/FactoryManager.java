package com.altnoir.abysslib.model.api.client.models;

import com.altnoir.abysslib.model.api.client.models.neoforge.FactoryManagerImpl;
import net.minecraft.resources.ResourceLocation;

/**
 * 模型工厂注册表（本库公开 API）。
 * <p>
 * 上游用 Architectury 的 {@code @ExpectPlatform} 分发到各平台实现；本库只服务 NeoForge，
 * 因此直接转发到 {@link FactoryManagerImpl}。
 */
public class FactoryManager {

    /**
     * Registers a new model factory, which will be used to create models for the given json.
     * @param id The id of the model factory
     * @param factory The factory to use
     */
    public static void register(ResourceLocation id, ALModelFactory factory) {
        FactoryManagerImpl.register(id, factory);
    }
}
