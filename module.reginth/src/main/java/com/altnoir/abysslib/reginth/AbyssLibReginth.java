package com.altnoir.abysslib.reginth;

import com.altnoir.abysslib.client.creative.ALSectionedCreativeTabRenderer;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * <b>AbyssLib - Reginth</b>（modid {@code abysslib_reginth}）：注册框架模块的客户端入口。
 * <p>
 * 装载时自动注册分区创造栏标题渲染（{@link ALSectionedCreativeTabRenderer}），
 * 消费方模组无需任何调用即可获得分区标题。横幅样式（纯色/贴图）在构建标签页时按页指定，
 * 见 {@code ALSectionedCreativeModeTab.configure(..., ALBannerStyle.xxx, ...)}。
 * <p>
 * <b>命名空间</b>：创造栏横幅贴图仍在冻结的 {@link #NAMESPACE}（{@code abysslib}）命名空间下，
 * 因为那是本库从一开始就发布出去的资源路径，改名会破坏既有资源包。
 */
@Mod(value = AbyssLibReginth.MOD_ID, dist = Dist.CLIENT)
public class AbyssLibReginth {
    /** 本模块的 modid（只能小写字母/数字/下划线）。 */
    public static final String MOD_ID = "abysslib_reginth";

    /** 本模块自有资源（创造栏横幅贴图）的命名空间 —— 冻结值，勿改。 */
    public static final String NAMESPACE = "abysslib";

    public static final Logger LOGGER = LogUtils.getLogger();

    public AbyssLibReginth(IEventBus modEventBus, ModContainer modContainer) {
        ALSectionedCreativeTabRenderer.register();
    }
}
