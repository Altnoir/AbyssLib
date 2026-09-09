package com.altnoir.abysslib.creative;

import net.minecraft.resources.ResourceLocation;

/**
 * 分区横幅样式：纯色或贴图。
 * <p>
 * 挂到 {@link ALSectionedCreativeModeTab} 上，渲染器按标签页读取，
 * 因此不同模组（甚至同一模组的不同标签页）可以各自指定互不影响的样式。
 * <p>
 * 用法（建标签页时作为第二个参数传入 configure）：
 * <pre>{@code
 * ALSectionedCreativeModeTab.configure(
 *         CreativeModeTab.builder()...,
 *         ALBannerStyle.colors(0xFF123456, 0xFF789ABC, 0xFFABCDEF, 0xFFFFFFFF), // 纯色
 *         MyItemGroups::populate, TS_ITEMS, TS_BLOCKS).build();
 *
 * ALSectionedCreativeModeTab.configure(
 *         CreativeModeTab.builder()...,
 *         ALBannerStyle.texture("mymod", "textures/gui/creative/banner"),       // 贴图
 *         MyItemGroups::populate, TS_ITEMS).build();
 * }</pre>
 * 不传样式时使用 {@link #DEFAULT}（AbyssLib 默认绿色系纯色）。
 */
public sealed interface ALBannerStyle permits ALBannerStyle.Colors, ALBannerStyle.Texture {

    /** AbyssLib 默认样式：绿色系纯色横幅。 */
    Colors DEFAULT = new Colors(0xFF182115, 0xFF64843A, 0xFF8CBA51, 0xFFB7D986);

    /** 纯色横幅。 */
    static Colors colors(int background, int borderMuted, int borderPrimary, int text) {
        return new Colors(background, borderMuted, borderPrimary, text);
    }

    /** 贴图横幅：整张贴图拉伸铺满横幅行（建议 PNG 尺寸 162×18）。 */
    static Texture texture(ResourceLocation texture) {
        return new Texture(texture);
    }

    /** 贴图横幅：写路径即可，如 {@code texture("mymod", "textures/gui/creative/banner")}。 */
    static Texture texture(String namespace, String path) {
        return new Texture(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /** 贴图横幅：写 "命名空间:路径" 字符串即可。 */
    static Texture texture(String texturePath) {
        int colon = texturePath.indexOf(':');
        if (colon <= 0 || colon == texturePath.length() - 1) {
            throw new IllegalArgumentException("Not a valid resource location: " + texturePath);
        }
        return texture(texturePath.substring(0, colon), texturePath.substring(colon + 1));
    }

    /** 纯色样式：颜色均为 ARGB（0xFF 开头即不透明）。 */
    record Colors(int background, int borderMuted, int borderPrimary, int text) implements ALBannerStyle {
    }

    /** 贴图样式：整张图拉伸铺满横幅行。 */
    record Texture(ResourceLocation texture) implements ALBannerStyle {
    }
}
