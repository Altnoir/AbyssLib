package com.altnoir.abysslib.client;

import com.altnoir.abysslib.AbyssLib;
import com.altnoir.abysslib.client.creative.ALSectionedCreativeTabRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * AbyssLib 客户端入口：装载时自动注册分区创造栏标题渲染
 * （{@link ALSectionedCreativeTabRenderer}），消费方模组无需任何调用即可获得分区标题。
 * 若需定制配色，可在自己的客户端初始化里调用
 * {@code ALSectionedCreativeTabRenderer.setPalette(...)}。
 */
@Mod(value = AbyssLib.MOD_ID, dist = Dist.CLIENT)
public class AbyssLibClient {
    public AbyssLibClient(IEventBus modEventBus) {
        ALSectionedCreativeTabRenderer.register();
    }
}
