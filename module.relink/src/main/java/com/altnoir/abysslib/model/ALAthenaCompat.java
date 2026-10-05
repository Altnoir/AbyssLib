package com.altnoir.abysslib.model;

import net.neoforged.fml.ModList;

/**
 * Reads Athena-style model definitions when the upstream mod is absent.
 */
public final class ALAthenaCompat {

    /**
     * 上游 Athena 的 modid。
     */
    public static final String ATHENA_MOD_ID = "athena";

    /**
     * 上游 Athena 使用的命名空间（键前缀 / 类型 id / 定义目录都用它）。
     */
    public static final String ATHENA_NAMESPACE = "athena";

    /**
     * 上游 Athena 的加载器声明键：{@code "athena:loader"}。
     */
    public static final String ATHENA_LOADER_KEY = ATHENA_NAMESPACE + ":loader";

    private static volatile Boolean enabled;

    private ALAthenaCompat() {
    }

    /**
     * Athena definitions are enabled unless the upstream mod is already loaded.
     */
    public static boolean enabled() {
        final Boolean cached = enabled;
        if (cached != null) {
            return cached;
        }
        final ModList modList = ModList.get();
        if (modList == null) {
            return true;
        }
        final boolean result = !modList.isLoaded(ATHENA_MOD_ID);
        enabled = result;
        if (result) {
            AbyssLibReLink.LOGGER.info(
                    "AbyssLib/ReLink: 未检测到上游 Athena -> 启用 athena:* 兼容层（Athena 格式的旧资源无需改写）");
        } else {
            AbyssLibReLink.LOGGER.info(
                    "AbyssLib/ReLink: 检测到上游 Athena 已加载 -> 已禁用 athena:* 兼容层，athena 格式资源交给上游处理");
        }
        return result;
    }
}
