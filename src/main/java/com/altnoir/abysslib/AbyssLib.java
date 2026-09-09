package com.altnoir.abysslib;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * AbyssLib —— Altnoir 系列模组的公共前置库。
 * 内置 Registrate 与 Simple Bedrock Model（jarJar），提供分区式创造栏与 {@code ALRegistrate} 辅助。
 * 本身不注册任何游戏内容。
 */
@Mod(AbyssLib.MOD_ID)
public class AbyssLib {
    public static final String MOD_ID = "abysslib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
