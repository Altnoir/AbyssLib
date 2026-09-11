package com.altnoir.abysslib;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * AbyssLib —— Altnoir 系列模组的公共前置库（<b>聚合模块</b>，modid {@code abysslib}）。
 * <p>
 * 本模块<b>自身不含任何功能代码</b>，它只做两件事：
 * <ol>
 *   <li>把三个功能模块的 jar 全部内嵌（{@code jarJar}）：{@code AbyssLib-Reginth}（注册框架）、
 *       {@code AbyssLib-ReLink}（贴图材质链接）、{@code AbyssLib-Atlas}（结构扩展）。
 *       用户只装这一个 jar 就能得到完整功能；开发者也可以只依赖单个模块。</li>
 *   <li>提供消费方一直在用的门面工具（{@link #modloc} 等 ResourceLocation / 注册表路径辅助）。</li>
 * </ol>
 * <p>
 * <b>只装单个模块时</b>：依赖里换成对应模块（例如 {@code AbyssLib-Atlas}）即可，
 * 此时本门面类不存在 —— 各模块有自己的入口类与命名空间常量
 * （{@code AbyssLibReLink.NAMESPACE} = {@code relink}、{@code AbyssLibAtlas.NAMESPACE} = {@code atlas}）。
 */
@Mod(AbyssLib.MOD_ID)
public class AbyssLib {
    /** 聚合模块的 modid。它同时是消费方 {@code neoforge.mods.toml} 里声明的依赖名。 */
    public static final String MOD_ID = "abysslib";

    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * NeoForge 入口。聚合模块不注册任何游戏内容，功能由内嵌的子模块各自的入口类负责
     * （{@code AbyssLibReginth} / {@code AbyssLibReLink} / {@code AbyssLibAtlas}）。
     */
    public AbyssLib(IEventBus modEventBus, ModContainer modContainer) {
        // 有意留空。
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
