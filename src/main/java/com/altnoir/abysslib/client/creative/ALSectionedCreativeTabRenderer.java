package com.altnoir.abysslib.client.creative;

import com.altnoir.abysslib.creative.ALBannerStyle;
import com.altnoir.abysslib.creative.ALSectionedCreativeModeTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

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
 * 用法：通常无需手动调用——AbyssLib 装载时已通过 {@code AbyssLibClient} 自动注册；
 * 也可在模组客户端入口显式调用一次（幂等）：
 * <pre>{@code
 * ALSectionedCreativeTabRenderer.register();
 * }</pre>
 */
public final class ALSectionedCreativeTabRenderer {
    private static final int VISIBLE_ROWS = 5;
    private static final int GRID_LEFT = 8;
    private static final int GRID_TOP = 17;
    private static final int GRID_WIDTH = 162;
    private static final int ROW_HEIGHT = 18;

    /** 贴图模式下叠加的分区标题文字颜色（白色 + 阴影，保证任意贴图上可读）。 */
    private static final int TEXTURE_TEXT_COLOR = 0xFFFFFFFF;

    private static boolean registered;

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
            drawBanner(graphics, y, tab.bannerStyle());
            drawTitle(graphics, font, section.title(), y, tab.bannerStyle());
        }
    }

    private static void drawBanner(GuiGraphics graphics, int top, ALBannerStyle style) {
        int width = style.pixelLength();
        if (style instanceof ALBannerStyle.Colors colors) {
            graphics.fill(GRID_LEFT, top, GRID_LEFT + width, top + ROW_HEIGHT, colors.background());
            graphics.fill(GRID_LEFT, top, GRID_LEFT + 1, top + ROW_HEIGHT, colors.borderMuted());
            graphics.fill(GRID_LEFT + width - 1, top, GRID_LEFT + width, top + ROW_HEIGHT, colors.borderMuted());
            graphics.fill(GRID_LEFT + 1, top, GRID_LEFT + width, top + 1, colors.borderPrimary());
            graphics.fill(GRID_LEFT + 1, top + ROW_HEIGHT - 1, GRID_LEFT + width, top + ROW_HEIGHT, colors.borderMuted());
        } else if (style instanceof ALBannerStyle.Texture texture) {
            // 整张贴图映射到 units×18 宽的横幅行：内置 banner_N 贴图尺寸与格数精确匹配（1:1），
            // 自定义贴图则整张拉伸到该宽度。
            graphics.blit(texture.texture(), GRID_LEFT, top, width, ROW_HEIGHT,
                    0.0F, 0.0F, GRID_WIDTH, ROW_HEIGHT, GRID_WIDTH, ROW_HEIGHT);
        }
    }

    private static void drawTitle(GuiGraphics graphics, Font font, Component title, int top, ALBannerStyle style) {
        if (style instanceof ALBannerStyle.Colors colors) {
            graphics.drawString(font, title, GRID_LEFT + 7, top + 5, colors.text(), false);
        } else if (style instanceof ALBannerStyle.Texture) {
            graphics.drawString(font, title, GRID_LEFT + 7, top + 5, TEXTURE_TEXT_COLOR, true);
        }
    }
}
