package com.altnoir.abysslib.hub;

import com.altnoir.abysslib.AbyssLib;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 聚合模块的<b>客户端入口</b>：只做一件事 —— 给 {@code abysslib} 注册一个配置界面工厂，
 * 让模组列表里 AbyssLib 的「配置」按钮打开 {@link ALConfigHubScreen}（统一配置入口）。
 *
 * <p>为什么单独一个类而不是塞进 {@link AbyssLib} 的构造器：{@link IConfigScreenFactory} 与
 * {@link ALConfigHubScreen} 都是<b>客户端类</b>，出现在双端构造器里会让专用服务端也去解析它们。
 * FML 允许同一个 modid 有多个 {@code @Mod} 类（{@code FMLModContainer} 内部是
 * {@code List<Class<?>> modClasses} 并逐个实例化），这里用 {@code dist = Dist.CLIENT} 限定只在客户端加载。
 */
@Mod(value = AbyssLib.MOD_ID, dist = Dist.CLIENT)
public class AbyssLibConfigHub {

    public AbyssLibConfigHub(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (minecraft, parent) -> new ALConfigHubScreen(parent));
    }
}
