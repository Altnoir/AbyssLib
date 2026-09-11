package com.altnoir.abysslib.model;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * <b>上游 Athena 写法兼容层</b>。
 * <p>
 * 本库的模型加载器移植自 <a href="https://github.com/terrarium-earth/Athena">Athena</a>，
 * 但把命名空间从 {@code athena} 改成了 {@link AbyssLibReLink#NAMESPACE}（{@code relink}）。
 * 为了让<b>既有 Athena 格式资源包不改一行就能用</b>，本类在运行时额外认这些上游写法：
 * <ul>
 *     <li>JSON 键 {@code "athena:loader"}（回退读取）</li>
 *     <li>模型类型 {@code athena:ctm} / {@code athena:carpet_ctm} / {@code athena:pane_ctm} /
 *         {@code athena:giant} / {@code athena:mural} / {@code athena:pillar} /
 *         {@code athena:limited_pillar} / {@code athena:pane_pillar}（同名别名注册）</li>
 *     <li>几何加载器 {@code "loader": "athena:athena"}（同名别名注册）</li>
 *     <li>定义目录 {@code assets/<命名空间>/athena/**.json}（一并扫描）</li>
 * </ul>
 * <p>
 * <b>不做兼容</b>：{@code athena:emissive} —— 上游 Athena 没有这个键，整模型发光是本库的扩展。
 * <p>
 * <b>与上游 Athena 共存</b>：若玩家已经装了上游 Athena 模组，上面的别名注册会与上游<b>抢同一个
 * 几何加载器 id</b>（{@code athena:athena}）从而冲突。因此本层用
 * {@link ModList#isLoaded(String)} 判定：<b>检测到上游在场就整体禁用</b>，那些资源交给上游处理。
 * 判定的结果是单向的——Athena 不认识 {@code relink:}，反之不成立。
 */
public final class ALAthenaCompat {

    /** 上游 Athena 的 modid。 */
    public static final String ATHENA_MOD_ID = "athena";

    /** 上游 Athena 使用的命名空间（键前缀 / 类型 id / 定义目录都用它）。 */
    public static final String ATHENA_NAMESPACE = "athena";

    /** 上游 Athena 的加载器声明键：{@code "athena:loader"}。 */
    public static final String ATHENA_LOADER_KEY = ATHENA_NAMESPACE + ":loader";

    /** 上游 Athena 的几何加载器 id：{@code athena:athena}（见上游 AthenaNeoForgeClient）。 */
    public static final ResourceLocation ATHENA_GEOMETRY_LOADER_ID =
            ResourceLocation.fromNamespaceAndPath(ATHENA_NAMESPACE, ATHENA_NAMESPACE);

    /** 惰性缓存：{@code null} = 还没问过 FML。 */
    private static volatile Boolean enabled;

    private ALAthenaCompat() {
    }

    /**
     * {@return 是否启用 {@code athena:*} 兼容} —— 默认启用；只有确认上游 Athena 已加载时才禁用。
     * <p>
     * 结果会缓存（模组列表在启动后不再变化）。若此刻 FML 尚未就绪（{@link ModList#get()} 为
     * {@code null}），按文档默认值返回 <b>启用</b> 且<b>不缓存</b>，下次再问。
     */
    public static boolean enabled() {
        final Boolean cached = enabled;
        if (cached != null) {
            return cached;
        }
        final ModList modList = ModList.get();
        if (modList == null) {
            // 模组列表还没就绪，无法判定；先按默认（启用）走，不留缓存
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
