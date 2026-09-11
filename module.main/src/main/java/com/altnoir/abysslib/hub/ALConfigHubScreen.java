package com.altnoir.abysslib.hub;

import com.altnoir.abysslib.AbyssLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * AbyssLib 聚合包的<b>统一配置入口</b>：列出所有「已加载且可配置」的 AbyssLib 模块，
 * 点进去打开的就是该模块<b>自己的标准配置界面</b>（直接复用 NeoForge 的
 * {@link ConfigurationScreen}，不重复实现任何界面）。
 *
 * <p><b>为什么是"跳转"而不是"一个界面里直读直写"</b>：FML 的配置文件是<b>按文件名全局独占</b>的
 * （{@code ConfigTracker.fileMap} 只按文件名做键，两个 mod 抢同名文件会抛异常崩游戏），
 * 所以聚合包无法"共享"子模块的配置文件；而把别的 mod 的配置项嵌进自己的界面又需要自建 UI。
 * 跳转式入口既给了用户"一个入口"，又不产生"两个配置来源"的隐患 —— 写入永远只发生在
 * 那个模块自己的文件里。
 *
 * <p>用户看到的是：模组列表 → {@code AbyssLib} → 配置 → 本界面 → 选模块 → 该模块的配置。
 * 只装单个模块（没装聚合包）时本界面不存在，直接用它自己的配置入口即可。
 */
public class ALConfigHubScreen extends Screen {

    /**
     * 聚合包只列本库自己的模块，不去枚举别人的 mod。
     * 顺序即显示顺序；没装的模块会被自动跳过。
     */
    private static final List<String> MODULE_MOD_IDS = List.of(
            "abysslib_reginth",
            "abysslib_relink",
            "abysslib_atlas");

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 6;

    private static final int COLOR_TITLE = 0xFFFFFF;
    private static final int COLOR_NOTE = 0xA0A0A0;

    /** 从哪个界面进来的（关闭时返回它）。 */
    private final Screen parent;

    public ALConfigHubScreen(Screen parent) {
        super(Component.translatableWithFallback("abysslib.config.hub.title", "AbyssLib Configuration"));
        this.parent = parent;
    }

    /**
     * 该模块是否有值得展示的配置：<b>有自己的配置文件</b>，或<b>自己注册了配置界面</b>。
     * 两者都没有的模块不在列表里出现（否则点进去只会看到一个空界面）。
     */
    private static boolean isConfigurable(ModContainer container) {
        if (!ModConfigs.getModConfigs(container.getModId()).isEmpty()) {
            return true;
        }
        return container.getCustomExtension(IConfigScreenFactory.class).isPresent();
    }

    /** {@return 已加载且可配置的 AbyssLib 模块（顺序同 {@link #MODULE_MOD_IDS}）} */
    public static List<ModContainer> discoverModules() {
        final List<ModContainer> modules = new ArrayList<>();
        final ModList modList = ModList.get();
        if (modList == null) {
            return modules;
        }
        for (String modId : MODULE_MOD_IDS) {
            modList.getModContainerById(modId)
                    .filter(ALConfigHubScreen::isConfigurable)
                    .ifPresent(modules::add);
        }
        return modules;
    }

    @Override
    protected void init() {
        final List<ModContainer> modules = discoverModules();
        AbyssLib.LOGGER.debug("[AbyssLib] 配置入口：发现 {} 个可配置模块 -> {}",
                modules.size(),
                modules.stream().map(m -> m.getModId()).toList());

        // 垂直居中排布：模块按钮若干 + Done
        final int rows = modules.size() + 1;
        final int totalHeight = rows * BUTTON_HEIGHT + (rows - 1) * BUTTON_SPACING;
        final int x = (this.width - BUTTON_WIDTH) / 2;
        int y = (this.height - totalHeight) / 2;

        for (ModContainer module : modules) {
            this.addRenderableWidget(Button.builder(
                            Component.literal(module.getModInfo().getDisplayName()),
                            button -> Minecraft.getInstance().setScreen(new ConfigurationScreen(module, this)))
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
            y += BUTTON_HEIGHT + BUTTON_SPACING;
        }

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds((this.width - 120) / 2, y, 120, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, COLOR_TITLE);
        if (discoverModules().isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatableWithFallback("abysslib.config.hub.none",
                            "No configurable AbyssLib module is installed."),
                    this.width / 2, this.height / 2, COLOR_NOTE);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
