package com.altnoir.abysslib.creative;

import com.altnoir.abysslib.reginth.AbyssLibReginth;
import net.minecraft.resources.ResourceLocation;

/**
 * 分区横幅样式：纯色或贴图，并支持自定义横幅长度。
 * <p>
 * 挂在 {@link ALSectionedCreativeModeTab} 上，渲染器按标签页读取，
 * 因此不同模组（甚至同一模组的不同标签页）可以各自指定互不影响的样式。
 * <p>
 * <b>长度以"格数"为单位</b>（每格 = 18px，即创造栏一格宽）：
 * 允许 1 ~ 9，写 9 即整行 162px。
 * 横幅 N 格时，该分区标题行行首 N 格被横幅占据，物品从右侧第 N+1 格同行接续排布。
 * <p>
 * 贴图统一走 {@link #texture(int)} / {@link #texture(int, String)}：
 * 只给格数 = 使用 AbyssLib 内置预设贴图（{@code banner_1.png ~ banner_9.png}，
 * 资源路径 {@code abysslib:textures/gui/section/banner_N.png}，宽 N*18、高 18，
 * 与格数精确匹配，1:1 显示）；给格数 + 自定义路径 = 用你自己的贴图并拉伸到该宽度。
 * <p>
 * 用法（建标签页时作为第二个参数传入 configure）：
 * <pre>{@code
 * // 纯色（默认 9 格 = 整行）：
 * ALSectionedCreativeModeTab.configure(
 *         CreativeModeTab.builder()...,
 *         ALBannerStyle.colors(0xFF123456, 0xFF789ABC, 0xFFABCDEF, 0xFFFFFFFF),
 *         MyItemGroups::populate, TS_ITEMS).build();
 *
 * // 内置预设贴图：只写格数（1~9，9 = 整行 162px）
 * ALSectionedCreativeModeTab.configure(
 *         CreativeModeTab.builder()...,
 *         ALBannerStyle.texture(4),                                  // → banner_4.png（72px）
 *         MyItemGroups::populate, TS_ITEMS).build();
 *
 * // 自定义贴图 + 格数：
 * ALSectionedCreativeModeTab.configure(
 *         CreativeModeTab.builder()...,
 *         ALBannerStyle.texture(3, "mymod:textures/gui/creative/banner"),
 *         MyItemGroups::populate, TS_ITEMS).build();
 * }</pre>
 * 不传样式时使用 {@link #DEFAULT}（AbyssLib 默认绿色系纯色，9 格整行）。
 */
public sealed interface ALBannerStyle permits ALBannerStyle.Colors, ALBannerStyle.Texture {

    /** 每格横幅的像素宽度（对应创造栏一格）。 */
    int UNITS_PIXEL = 18;

    /** 最大格数（9 格 = 创造栏整行 162px）。 */
    int MAX_UNITS = 9;

    /** 最小格数。 */
    int MIN_UNITS = 1;

    /** AbyssLib 默认样式：绿色系纯色横幅，9 格整行。 */
    Colors DEFAULT = new Colors(MAX_UNITS, 0xFF182115, 0xFF64843A, 0xFF8CBA51, 0xFFB7D986);

    /** 本样式占的横幅格数（1~9）。 */
    int units();

    /** 横幅实际像素宽度 = units() * 18。 */
    default int pixelLength() {
        return units() * UNITS_PIXEL;
    }

    /** 校验格数合法性并返回。 */
    static int validateUnits(int units) {
        if (units < MIN_UNITS || units > MAX_UNITS) {
            throw new IllegalArgumentException(
                    "Banner units must be between " + MIN_UNITS + " and " + MAX_UNITS + ", got " + units);
        }
        return units;
    }

    // ---------- 纯色 ----------

    /** 纯色横幅，默认 9 格（整行 162）。 */
    static Colors colors(int background, int borderMuted, int borderPrimary, int text) {
        return new Colors(MAX_UNITS, background, borderMuted, borderPrimary, text);
    }

    /** 纯色横幅并指定格数（1~9，9 = 整行）。 */
    static Colors colors(int units, int background, int borderMuted, int borderPrimary, int text) {
        return new Colors(units, background, borderMuted, borderPrimary, text);
    }

    // ---------- 贴图 ----------

    /**
     * 内置预设贴图：按格数取 {@code abysslib:textures/gui/section/banner_N.png}，
     * N = 格数（1→banner_1=18px … 9→banner_9=162px），尺寸与格数精确匹配。
     */
    static Texture texture(int units) {
        int u = validateUnits(units);
        return new Texture(u, ResourceLocation.fromNamespaceAndPath(AbyssLibReginth.NAMESPACE, "textures/gui/section/banner_" + u));
    }

    /** 自定义贴图并指定格数：路径写 "命名空间:路径" 或 ResourceLocation。 */
    static Texture texture(int units, String texturePath) {
        return new Texture(validateUnits(units), resource(texturePath));
    }

    /** 自定义贴图并指定格数。 */
    static Texture texture(int units, ResourceLocation texture) {
        return new Texture(validateUnits(units), texture);
    }

    private static ResourceLocation resource(String texturePath) {
        int colon = texturePath.indexOf(':');
        if (colon <= 0 || colon == texturePath.length() - 1) {
            throw new IllegalArgumentException("Not a valid resource location: " + texturePath);
        }
        return ResourceLocation.fromNamespaceAndPath(texturePath.substring(0, colon), texturePath.substring(colon + 1));
    }

    /** 纯色样式：颜色均为 ARGB（0xFF 开头即不透明）。 */
    record Colors(int units, int background, int borderMuted, int borderPrimary, int text) implements ALBannerStyle {
        public Colors {
            units = ALBannerStyle.validateUnits(units);
        }

        /** 生成指定格数的副本（1~9，9 = 整行）。 */
        public Colors withUnits(int newUnits) {
            return new Colors(newUnits, background, borderMuted, borderPrimary, text);
        }
    }

    /** 贴图样式：整张贴图拉伸铺满指定格数的横幅行。 */
    record Texture(int units, ResourceLocation texture) implements ALBannerStyle {
        public Texture {
            units = ALBannerStyle.validateUnits(units);
        }

        /** 生成指定格数的副本（配合尺寸为 N×18 的贴图可 1:1 显示）。 */
        public Texture withUnits(int newUnits) {
            return new Texture(newUnits, texture);
        }
    }
}
