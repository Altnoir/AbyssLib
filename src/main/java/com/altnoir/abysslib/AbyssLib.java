package com.altnoir.abysslib;

import com.altnoir.abysslib.structure.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

/**
 * AbyssLib —— Altnoir 系列模组的公共前置库。
 * **源码级内置**注册框架 {@code Reginth}（fork 自 Registrate，见 {@code com.altnoir.abysslib.reginth}），
 * 提供分区式创造栏；另源码内置 CTM / 动态模型加载器（移植自 Athena）。
 * 本身不注册任何游戏内容。
 * <p>
 * 同时提供一组 ResourceLocation / 注册表路径工具（{@link #loc} 等），
 * 供依赖本库的模组直接调用以替代各自重复的实现。
 */
@Mod(AbyssLib.MOD_ID)
public class AbyssLib {
    public static final String MOD_ID = "abysslib";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * NeoForge 入口。当前只做一件事：把"原版结构限制放宽"的启动自检挂到 mod 事件总线上
     * （见 {@link ALStructureEnhancementCheck}）。
     * <p>用构造器显式注册而非 {@code @EventBusSubscriber}，是为了避开 NeoForge 21.1 中
     * {@code EventBusSubscriber.Bus} 的弃用告警，也避免依赖"总路由事件类型自动推断"的行为。
     */
    public AbyssLib(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ALStructureEnhancementCheck::onCommonSetup);

        // 游戏总线：世界数据包加载完成后二次汇报（verifyRange 这类注入要等数据包解析才会置位）
        NeoForge.EVENT_BUS.addListener(ALStructureEnhancementCheck::onServerStarted);

        // 原版世界生成扩展：abysslib:per_chunk（放置）、abysslib:grid_profile（数据包注册表）、abysslib:jigsaw（结构类型）
        modEventBus.addListener(ALGridProfile::registerDataPackRegistry);
        ALStructurePlacements.register(modEventBus);
        ALStructureTypes.register(modEventBus);

        // 数据包重载后清空布局缓存（profile / 结构 JSON 可能已变）
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> ALStructureLayoutCache.clear());
    }

    /**
     * 生成 {@code abysslib:<path>} 资源路径。
     * 注意：只应给 abysslib 自己的资源用；消费方模组请用
     * {@code AbyssLib.modloc(MyMod.MOD_ID, path)} 或保留各自入口类的 loc。
     */
    public static ResourceLocation loc(String path) {
        return modloc(MOD_ID, path);
    }

    /** 生成 {@code namespace:path} 资源路径。 */
    public static ResourceLocation modloc(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /** 生成原版命名空间（minecraft）资源路径。 */
    public static ResourceLocation mcloc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    /** 解析 "namespace:path" 字符串（非法则抛异常）。 */
    public static ResourceLocation parse(String location) {
        return ResourceLocation.parse(location);
    }

    /** 解析 "namespace:path" 字符串（非法返回 null）。 */
    public static ResourceLocation tryParse(String location) {
        return ResourceLocation.tryParse(location);
    }

    /** 取物品注册名的 path 段。 */
    public static String getItemPath(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    /** 取方块注册名的 path 段。 */
    public static String getBlockPath(Block block) {
        return getBlockKey(block).getPath();
    }

    /** 取方块的注册名（ResourceLocation）。 */
    public static ResourceLocation getBlockKey(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }
}
