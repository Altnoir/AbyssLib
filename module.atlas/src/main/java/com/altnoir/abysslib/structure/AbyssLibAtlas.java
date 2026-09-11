package com.altnoir.abysslib.structure;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

/**
 * <b>AbyssLib - Atlas</b>（modid {@code abysslib_atlas}）：结构扩展模块的双端入口。
 * <p>
 * 负责：原版结构上限放宽的自检、{@code atlas:per_chunk}（放置）、{@code atlas:grid_profile}
 * （数据包注册表）、{@code atlas:jigsaw}（结构类型）的注册，以及数据包重载后清空布局缓存。
 * <p>
 * 用构造器显式注册而非 {@code @EventBusSubscriber}，是为了避开 NeoForge 21.1 中
 * {@code EventBusSubscriber.Bus} 的弃用告警，也避免依赖"总路由事件类型自动推断"的行为。
 * <p>
 * <b>命名空间</b>：本模块的注册表 id 一律用 {@link #NAMESPACE}（{@code atlas}），
 * 与 modid（{@code abysslib_atlas}）是两回事。
 */
@Mod(AbyssLibAtlas.MOD_ID)
public class AbyssLibAtlas {
    /** 本模块的 modid（只能小写字母/数字/下划线）。 */
    public static final String MOD_ID = "abysslib_atlas";

    /** 本模块拥有的注册表命名空间。数据包/结构 JSON 里看到的是这个名字。 */
    public static final String NAMESPACE = "atlas";

    public static final Logger LOGGER = LogUtils.getLogger();

    public AbyssLibAtlas(IEventBus modEventBus, ModContainer modContainer) {
        // 启动自检：第一个时机验"静态初始化期就执行"的注入
        modEventBus.addListener(ALStructureEnhancementCheck::onCommonSetup);

        // 游戏总线：世界数据包加载完成后二次汇报（verifyRange 这类注入要等数据包解析才会置位）
        NeoForge.EVENT_BUS.addListener(ALStructureEnhancementCheck::onServerStarted);

        // 原版世界生成扩展：atlas:per_chunk（放置）、atlas:grid_profile（数据包注册表）、atlas:jigsaw（结构类型）
        modEventBus.addListener(ALGridProfile::registerDataPackRegistry);
        ALStructurePlacements.register(modEventBus);
        ALStructureTypes.register(modEventBus);

        // 数据包重载后清空布局缓存（profile / 结构 JSON 可能已变）
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> ALStructureLayoutCache.clear());
    }
}
