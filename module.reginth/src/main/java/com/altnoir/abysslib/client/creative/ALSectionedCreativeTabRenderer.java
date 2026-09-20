package com.altnoir.abysslib.client.creative;

import com.altnoir.abysslib.creative.ALBannerStyle;
import com.altnoir.abysslib.creative.ALCreativeTabSection;
import com.altnoir.abysslib.creative.ALSectionedCreativeModeTab;
import com.altnoir.abysslib.creative.ALTitlePlate;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * 分区创造栏标题渲染器（客户端）。
 * <p>
 * 当玩家打开创造栏、当前选中的是 {@link ALSectionedCreativeModeTab} 时，
 * 在其空行分隔行上绘制分区标题横幅。样式由每个标签页自己的
 * {@link ALBannerStyle} 决定（纯色或贴图，见构建时 {@code configure(...)} 的第二个参数）；
 * 不指定则用 AbyssLib 默认绿色系 {@link ALBannerStyle#DEFAULT}。
 * 依赖 {@code accesstransformer.cfg} 放宽了对 {@code CreativeModeInventoryScreen}
 * {@code selectedTab}/{@code scrollOffs} 两个字段的访问权限。
 * <p>
 * AbyssLib 内置的 {@code banner_N} 预设贴图在首次使用时<b>直接从本库类路径读取并注册到
 * TextureManager</b>（不依赖资源包/资产服务），确保无论 dev/生产都能取到。
 * <p>
 * 用法：通常无需手动调用——AbyssLib 装载时已通过 {@code AbyssLibClient} 自动注册；
 * 也可在模组客户端入口显式调用一次（幂等）：
 * <pre>{@code
 * ALSectionedCreativeTabRenderer.register();
 * }</pre>
 */
public final class ALSectionedCreativeTabRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ALSectionedCreativeTabRenderer.class);

    private static final int VISIBLE_ROWS = 5;
    private static final int GRID_LEFT = 8;
    private static final int GRID_TOP = 17;
    private static final int GRID_WIDTH = 162;
    private static final int ROW_HEIGHT = 18;

    /**
     * 贴图模式下叠加的分区标题文字颜色（白色 + 阴影，保证任意贴图上可读）。
     */
    private static final int TEXTURE_TEXT_COLOR = 0xFFFFFFFF;

    /**
     * 标题文字相对横幅左上角的偏移。
     */
    private static final int TITLE_TEXT_OFFSET_X = 7;
    private static final int TITLE_TEXT_OFFSET_Y = 5;

    /**
     * 标题超宽被截断时补的省略号。
     * 标题底板要不要、什么颜色/多不透明，见 {@link ALCreativeTabSection#titlePlate(ALTitlePlate)}，
     * <b>默认不启用</b>（{@link ALTitlePlate#DISABLED}）。
     */
    private static final String TITLE_ELLIPSIS = "…";

    private static boolean registered;

    /**
     * 已从类路径注册过的内置预设贴图。
     */
    private static final Set<ResourceLocation> REGISTERED_PRESETS = new HashSet<>();

    private ALSectionedCreativeTabRenderer() {
    }

    /**
     * 在 NeoForge 游戏总线上注册渲染监听（幂等，可安全重复调用）。
     * 必须在客户端环境调用，服务端调用会因缺少客户端类而失败。
     */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(ALSectionedCreativeTabRenderer::onRenderForeground);
    }

    /**
     * 把"普通资源路径"补成 MC 资产加载要求的带扩展名形式。
     * <p>
     * <b>为什么必须有这一步</b>：{@code RenderSystem.setShaderTexture(0, loc)} 对**非图集**贴图走
     * {@code SimpleTexture}，它把 {@code loc} 的路径原样交给资源管理器，<b>不会自己补 {@code .png}</b>。
     * 于是写 {@code mia:textures/gui/ctab/building} 会直接
     * {@code FileNotFoundException}，在游戏里就是一块黑紫。
     * <p>
     * 本库对外的约定是写"普通资源路径"（见 {@link ALCreativeTabSection} 的构造器注释），
     * 所以这里统一补上 {@code .png}；已经带 {@code .png} 的原样返回，
     * 两种写法都能用（{@code abysslib:} 内置预设贴图不受影响，见 {@link #ensurePresetTexture}）。
     */
    private static ResourceLocation assetTexture(ResourceLocation texture) {
        if (texture.getPath().endsWith(".png")) {
            return texture;
        }
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + ".png");
    }

    /**
     * 确保内置预设贴图（abysslib 命名空间）已注册进 TextureManager：
     * 从本类所在 jar 的类路径直接读取 PNG（路径 {@code /assets/abysslib/textures/gui/section/banner_N.png}），
     * 解码为 {@link NativeImage} 后注册到原 ResourceLocation 上。
     * 这样即使客户端资源包/资产服务未把 abysslib 的 assets 挂载进来也能绘制。
     */
    private static void ensurePresetTexture(ResourceLocation loc) {
        if (!loc.getNamespace().equals("abysslib") || !REGISTERED_PRESETS.add(loc)) {
            return;
        }
        // 调用方已通过 assetTexture() 保证路径带 .png
        String classPath = "/assets/" + loc.getNamespace() + "/" + loc.getPath();
        try (InputStream in = ALSectionedCreativeTabRenderer.class.getResourceAsStream(classPath)) {
            if (in == null) {
                LOGGER.error("AbyssLib preset banner texture missing on classpath: {}", classPath);
                return;
            }
            NativeImage image = NativeImage.read(in);
            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(image));
            LOGGER.debug("Registered AbyssLib preset banner texture {}", loc);
        } catch (Exception e) {
            LOGGER.error("Failed to register AbyssLib preset banner texture {}", loc, e);
        }
    }

    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        if (event.getContainerScreen() instanceof CreativeModeInventoryScreen screen
                && CreativeModeInventoryScreen.selectedTab instanceof ALSectionedCreativeModeTab tab) {
            render(event.getGuiGraphics(), tab, screen.scrollOffs);
        }
    }

    private static void render(GuiGraphics graphics, ALSectionedCreativeModeTab tab, float scrollOffset) {
        int firstVisibleRow = tab.visibleStartRow(scrollOffset);
        Font font = Minecraft.getInstance().font;

        for (ALSectionedCreativeModeTab.SectionLayout section : tab.sectionLayouts()) {
            int visibleRow = section.headingRow() - firstVisibleRow;
            if (visibleRow < 0 || visibleRow >= VISIBLE_ROWS) {
                continue;
            }
            int y = GRID_TOP + visibleRow * ROW_HEIGHT;
            if (section.hasBannerTexture()) {
                // 自带横幅贴图的分区：横幅占行首 bannerUnits 格（左对齐），物品从右边同行接续。
                // 标题画在横幅最左侧；贴图里的图案/字一般在右半边，左侧留白给标题。
                drawBannerTexture(graphics, y, section.bannerTexture(), section.bannerPixelWidth());
                drawTitle(graphics, font, section.title(), y, section.bannerPixelWidth(),
                        TEXTURE_TEXT_COLOR, true, section.titlePlate());
            } else {
                drawBanner(graphics, y, tab.bannerStyle(), section.bannerPixelWidth());
                drawTitle(graphics, font, section.title(), y, section.bannerPixelWidth(),
                        bannerTextColor(tab.bannerStyle()), isTextureBanner(tab.bannerStyle()),
                        section.titlePlate());
            }
        }
    }

    /**
     * 画分区自带的横幅贴图，占 {@code width} 像素宽（= 格数 × 18）、一行高。
     * 只有 AbyssLib 自己的 {@code abysslib:} 预设贴图需要从类路径手工注册；
     * 其它命名空间（如 {@code mia:}）走正常资源包加载。
     * <p>
     * 贴图会被拉伸到 {@code width}；想 1:1 就把贴图做成 {@code 格数 × 18} 宽。
     */
    private static void drawBannerTexture(GuiGraphics graphics, int top, ResourceLocation texture, int width) {
        ResourceLocation tex = assetTexture(texture);
        ensurePresetTexture(tex);
        RenderSystem.setShaderTexture(0, tex);
        graphics.blit(tex, GRID_LEFT, top, width, ROW_HEIGHT,
                0.0F, 0.0F, width, ROW_HEIGHT, width, ROW_HEIGHT);
        graphics.flush();
    }

    private static void drawBanner(GuiGraphics graphics, int top, ALBannerStyle style, int width) {
        if (style instanceof ALBannerStyle.Colors colors) {
            graphics.fill(GRID_LEFT, top, GRID_LEFT + width, top + ROW_HEIGHT, colors.background());
            graphics.fill(GRID_LEFT, top, GRID_LEFT + 1, top + ROW_HEIGHT, colors.borderMuted());
            graphics.fill(GRID_LEFT + width - 1, top, GRID_LEFT + width, top + ROW_HEIGHT, colors.borderMuted());
            graphics.fill(GRID_LEFT + 1, top, GRID_LEFT + width, top + 1, colors.borderPrimary());
            graphics.fill(GRID_LEFT + 1, top + ROW_HEIGHT - 1, GRID_LEFT + width, top + ROW_HEIGHT, colors.borderMuted());
        } else if (style instanceof ALBannerStyle.Texture texture) {
            // 内置预设贴图：先确保从类路径注册过；自定义贴图走正常 MC 资产加载。
            ResourceLocation tex = assetTexture(texture.texture());
            ensurePresetTexture(tex);
            // 整张贴图映射到 units×18 宽的横幅行：内置 banner_N 贴图尺寸与格数精确匹配（1:1），
            // 自定义贴图则整张拉伸到该宽度。
            // 显式绑定 + 立即 flush：避免 GUI 纹理纹理批次里贴图未绑定导致的黑紫块。
            RenderSystem.setShaderTexture(0, tex);
            graphics.blit(tex, GRID_LEFT, top, width, ROW_HEIGHT,
                    0.0F, 0.0F, width, ROW_HEIGHT, width, ROW_HEIGHT);
            graphics.flush();
        }
    }

    private static boolean isTextureBanner(ALBannerStyle style) {
        return style instanceof ALBannerStyle.Texture;
    }

    private static int bannerTextColor(ALBannerStyle style) {
        return style instanceof ALBannerStyle.Colors colors ? colors.text() : TEXTURE_TEXT_COLOR;
    }

    /**
     * 画分区标题：可选先垫一层底板（{@link ALTitlePlate}，<b>默认不启用</b>），再画文字。
     * <p>
     * 层次是刻意的 —— <b>底板在横幅之上、文字之下</b>：贴图的渐变/花纹先被压暗，
     * 白字再压上去，任意底纹上都读得清。底板宽度 = {@code font.width(title) + 2 * padX}，
     * 高度 = {@code font.lineHeight + 2 * padY}，跟着标题长度走。
     * <p>
     * 标题会被限制在横幅宽度内：超长时截断并加省略号，避免压到右边的物品上
     * （横幅只有 4 格 = 72px 时英文标题很容易超宽）。
     */
    private static void drawTitle(GuiGraphics graphics, Font font, Component title, int top,
                                  int bannerWidth, int textColor, boolean shadow, ALTitlePlate plate) {
        int textX = GRID_LEFT + TITLE_TEXT_OFFSET_X;
        int textY = top + TITLE_TEXT_OFFSET_Y;
        // 右边留出与左边一样的边距，标题不会贴到横幅边缘
        int maxTextWidth = Math.max(bannerWidth - TITLE_TEXT_OFFSET_X * 2, 0);
        Component shown = fitTitle(font, title, maxTextWidth);
        int width = font.width(shown);
        if (width <= 0) {
            return;
        }

        if (plate.enabled()) {
            graphics.fill(
                    textX - plate.padX(),
                    textY - plate.padY(),
                    textX + width + plate.padX(),
                    textY + font.lineHeight + plate.padY(),
                    plate.argbColor());
        }
        graphics.drawString(font, shown, textX, textY, textColor, shadow);
    }

    /**
     * 标题超宽时截断并加省略号；连省略号都放不下就返回空（不画）。
     */
    private static Component fitTitle(Font font, Component title, int maxWidth) {
        if (maxWidth <= 0) {
            return Component.empty();
        }
        if (font.width(title) <= maxWidth) {
            return title;
        }
        int allowed = maxWidth - font.width(TITLE_ELLIPSIS);
        if (allowed <= 0) {
            return Component.empty();
        }
        return Component.literal(font.substrByWidth(title, allowed).getString() + TITLE_ELLIPSIS);
    }
}