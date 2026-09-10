package com.altnoir.abysslib.client;

import com.altnoir.abysslib.AbyssLib;
import com.altnoir.abysslib.model.neoforge.client.ALModelSetup;
import com.altnoir.abysslib.client.creative.ALSectionedCreativeTabRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * AbyssLib 客户端入口：装载时自动注册分区创造栏标题渲染
 * （{@link ALSectionedCreativeTabRenderer}），消费方模组无需任何调用即可获得分区标题。
 * 横幅样式（纯色/贴图）在构建标签页时按页指定，见
 * {@code ALSectionedCreativeModeTab.configure(..., ALBannerStyle.xxx, ...)}。
 * <p>
 * 同时初始化内置模型加载器（CTM / 动态模型，见 {@link ALModelSetup}）：
 * 注册默认模型类型、几何加载器 {@code abysslib:model}、顶层模型替换事件，
 * 以及 OptiFine 式发光叠加层的客户端配置（{@link ALClientConfig}）。
 * 全部为客户端内容，专用服务端不会加载（{@code @Mod(dist = CLIENT)}）。
 */
@Mod(value = AbyssLib.MOD_ID, dist = Dist.CLIENT)
public class AbyssLibClient {
    public AbyssLibClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ALClientConfig.CLIENT_SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(AbyssLibClient::onConfigLoad);

        ALSectionedCreativeTabRenderer.register();
        ALModelSetup.init(modEventBus);
    }

    private static void onConfigLoad(ModConfigEvent event) {
        final ALClientConfig.Reload reload = ALClientConfig.onLoad(event.getConfig());
        if (reload == ALClientConfig.Reload.NONE || !(event instanceof ModConfigEvent.Reloading)) {
            return;
        }
        // 运行中改动了发光叠加层设置：区块网格里已经烘焙了旧的 quad，必须让客户端重算才能立刻生效
        // （否则玩家关掉开关后矿物还会继续发光）。
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        if (reload == ALClientConfig.Reload.MODELS) {
            // 关闭 → 开启：模型之前没被包装，必须重新烘焙模型
            AbyssLib.LOGGER.info("AbyssLib: emissive overlay enabled, reloading client resources");
            minecraft.execute(minecraft::reloadResourcePacks);
        } else if (minecraft.level != null) {
            // 关闭、或改后缀/排除表：重建区块网格即可（不重载图集，代价小得多）
            AbyssLib.LOGGER.info("AbyssLib: emissive overlay settings changed, rebuilding chunks");
            minecraft.execute(() -> minecraft.levelRenderer.allChanged());
        }
    }
}
