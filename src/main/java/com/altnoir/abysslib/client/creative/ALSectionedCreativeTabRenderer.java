package com.altnoir.abysslib.client.creative;

import com.altnoir.abysslib.creative.ALSectionedCreativeModeTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 分区创造栏标题渲染器（客户端）。
 * <p>
 * 当玩家打开创造栏、当前选中的是 {@link ALSectionedCreativeModeTab} 时，
 * 在其空行分隔行上绘制分区标题横幅（颜色可经 {@link #setPalette} 定制）。
 * 依赖 {@code accesstransformer.cfg} 放宽了对 {@code CreativeModeInventoryScreen}
 * {@code selectedTab}/{@code scrollOffs} 两个字段的访问权限。
 * <p>
 * 用法（模组客户端入口调用一次即可，重复调用幂等）：
 * <pre>{@code
 * ALSectionedCreativeTabRenderer.register();
 * }</pre>
 * 若在 AbyssLib 自身装载时已通过 {@code AbyssLibClient} 自动注册，则无需再次调用。
 */
public final class ALSectionedCreativeTabRenderer {
    private static final int VISIBLE_ROWS = 5;
    private static final int GRID_LEFT = 8;
    private static final int GRID_TOP = 17;
    private static final int GRID_WIDTH = 162;
    private static final int ROW_HEIGHT = 18;

    private static boolean registered;
    private static Palette palette = Palette.DEFAULT;

    private ALSectionedCreativeTabRenderer() {
    }

    /** 分区横幅配色。 */
    public record Palette(int background, int borderMuted, int borderPrimary, int text) {
        /** 默认暖棕色配色（与 Altnoir 模组现有风格一致）。 */
        public static final Palette DEFAULT = new Palette(
                0xFF4A3728,
                0xFF6B5440,
                0xFF8B7355,
                0xFFD4C4A8
        );
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

    /** 定制横幅配色（全局生效，后调用者覆盖）。 */
    public static void setPalette(Palette palette) {
        ALSectionedCreativeTabRenderer.palette = palette;
    }

    public static Palette palette() {
        return palette;
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
            renderBanner(graphics, y);
            graphics.drawString(font, section.title(), GRID_LEFT + 7, y + 5, palette.text(), false);
        }
    }

    private static void renderBanner(GuiGraphics graphics, int top) {
        graphics.fill(GRID_LEFT, top, GRID_LEFT + GRID_WIDTH, top + ROW_HEIGHT, palette.background());
        graphics.fill(GRID_LEFT, top, GRID_LEFT + 1, top + ROW_HEIGHT, palette.borderMuted());
        graphics.fill(GRID_LEFT + GRID_WIDTH - 1, top, GRID_LEFT + GRID_WIDTH, top + ROW_HEIGHT, palette.borderMuted());
        graphics.fill(GRID_LEFT + 1, top, GRID_LEFT + GRID_WIDTH, top + 1, palette.borderPrimary());
        graphics.fill(GRID_LEFT + 1, top + ROW_HEIGHT - 1, GRID_LEFT + GRID_WIDTH, top + ROW_HEIGHT, palette.borderMuted());
    }
}
