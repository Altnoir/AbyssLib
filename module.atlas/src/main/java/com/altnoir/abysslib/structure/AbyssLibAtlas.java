package com.altnoir.abysslib.structure;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.slf4j.Logger;

// 注册 Atlas 世界生成类型，并在数据包重载后清理布局缓存。
@Mod(AbyssLibAtlas.MOD_ID)
public class AbyssLibAtlas {
    public static final String MOD_ID = "abysslib_atlas";

    public static final String NAMESPACE = "atlas";

    public static final Logger LOGGER = LogUtils.getLogger();

    public AbyssLibAtlas(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ALStructureEnhancementCheck::onCommonSetup);

        NeoForge.EVENT_BUS.addListener(ALStructureEnhancementCheck::onServerStarted);

        modEventBus.addListener(ALGridProfile::registerDataPackRegistry);
        ALStructurePlacements.register(modEventBus);
        ALStructureTypes.register(modEventBus);

        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) -> ALStructureLayoutCache.clear());
    }
}
