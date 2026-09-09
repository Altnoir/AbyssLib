package com.altnoir.abysslib;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * AbyssLib —— Altnoir 系列模组的公共前置库。
 * 内置 Registrate 与 Simple Bedrock Model（jarJar），提供分区式创造栏与 {@code ALRegistrate} 辅助。
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
