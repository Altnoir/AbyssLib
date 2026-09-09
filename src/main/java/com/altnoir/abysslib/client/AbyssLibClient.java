package com.altnoir.abysslib.client;

import com.altnoir.abysslib.AbyssLib;
import com.altnoir.abysslib.client.creative.ALSectionedCreativeTabRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * AbyssLib 客户端入口：装载时自动注册分区创造栏标题渲染
 * （{@link ALSectionedCreativeTabRenderer}），消费方模组无需任何调用即可获得分区标题。
 * 横幅样式（纯色/贴图）在构建标签页时按页指定，见
 * {@code ALSectionedCreativeModeTab.configure(..., ALBannerStyle.xxx, ...)}。
 */
@Mod(value = AbyssLib.MOD_ID, dist = Dist.CLIENT)
public class AbyssLibClient {
    public AbyssLibClient(IEventBus modEventBus) {
        ALSectionedCreativeTabRenderer.register();
    }
}
