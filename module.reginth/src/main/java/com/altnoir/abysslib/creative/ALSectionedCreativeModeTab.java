package com.altnoir.abysslib.creative;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 分区式创造栏（CreativeModeTab 子类）。
 * 通过 {@link #configure} 把若干 {@link ALCreativeTabSection} 挂到标签页上：
 * 构建时按分区顺序输出条目，同一物品去重。
 * 横幅 N 格 = 分区标题行行首 N 格被横幅占据，物品从横幅右侧同行接续排布
 * （N=9 时横幅独占一整行、物品从下一行开始，即原版式布局）。
 */
public final class ALSectionedCreativeModeTab extends CreativeModeTab {
    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 5;

    private final List<ALCreativeTabSection> sections;
    private final Consumer<ItemDisplayParameters> populator;
    private final ALBannerStyle bannerStyle;
    private Collection<ItemStack> displayItems = List.of();
    private Set<ItemStack> searchItems = ItemStackLinkedSet.createTypeAndComponentsSet();
    private List<SectionLayout> sectionLayouts = List.of();
    @Nullable
    private ItemDisplayParameters cachedParameters;

    private ALSectionedCreativeModeTab(Builder builder, List<ALCreativeTabSection> sections, ALBannerStyle bannerStyle, Consumer<ItemDisplayParameters> populator) {
        super(builder);
        this.sections = List.copyOf(sections);
        this.bannerStyle = bannerStyle;
        this.populator = populator;
    }

    /** 使用 AbyssLib 默认横幅样式（{@link ALBannerStyle#DEFAULT}）构建。 */
    public static Builder configure(Builder builder, Consumer<ItemDisplayParameters> populator, ALCreativeTabSection... sections) {
        return configure(builder, ALBannerStyle.DEFAULT, populator, sections);
    }

    /**
     * 指定横幅样式构建：每个标签页可独立使用自己的纯色或贴图横幅
     * （见 {@link ALBannerStyle}），不指定则用默认样式。
     */
    public static Builder configure(Builder builder, ALBannerStyle bannerStyle, Consumer<ItemDisplayParameters> populator, ALCreativeTabSection... sections) {
        List<ALCreativeTabSection> sectionList = List.of(sections);
        return builder.withTabFactory(tabBuilder -> new ALSectionedCreativeModeTab(tabBuilder, sectionList, bannerStyle, populator));
    }

    /** 本标签页使用的横幅样式（渲染器按此绘制分区标题行）。 */
    public ALBannerStyle bannerStyle() {
        return bannerStyle;
    }

    @Override
    public void buildContents(ItemDisplayParameters parameters) {
        this.cachedParameters = parameters;
        sections.forEach(ALCreativeTabSection::clear);
        populator.accept(parameters);

        List<ItemStack> newDisplayItems = new ArrayList<>();
        Set<ItemStack> newSearchItems = ItemStackLinkedSet.createTypeAndComponentsSet();
        Set<ItemStack> seenDisplayItems = ItemStackLinkedSet.createTypeAndComponentsSet();
        List<SectionLayout> newLayouts = new ArrayList<>();

        for (ALCreativeTabSection section : sections) {
            List<ItemStack> enabledItems = section.itemStacks().stream()
                    .filter(stack -> stack.getItem().isEnabled(parameters.enabledFeatures()))
                    .filter(seenDisplayItems::add)
                    .toList();
            if (enabledItems.isEmpty()) {
                continue;
            }

            // 横幅 N 格 = 该行行首 N 格为空（渲染器在此画横幅），物品从第 N+1 格同行接续；
            // N=9 时横幅独占一整行、物品从下一行开始（与原版/默认行为一致）。
            int columns = bannerStyle().units();

            int headingRow = newDisplayItems.size() / COLUMNS;
            newLayouts.add(new SectionLayout(section.title(), headingRow));
            if (columns < COLUMNS) {
                // 横幅只占行首 N 格：留出 N 个空位，物品接着往后排
                for (int i = 0; i < columns; i++) {
                    newDisplayItems.add(ItemStack.EMPTY);
                }
            } else {
                // 整行横幅（独占一行）
                addEmptyRow(newDisplayItems);
            }
            newSearchItems.addAll(enabledItems);
            newDisplayItems.addAll(enabledItems);
            padToCompleteRow(newDisplayItems);
        }

        displayItems = List.copyOf(newDisplayItems);
        searchItems = newSearchItems;
        sectionLayouts = List.copyOf(newLayouts);
    }

    public void rebuild() {
        if (cachedParameters != null) {
            buildContents(cachedParameters);
        }
    }

    @Override
    public Collection<ItemStack> getDisplayItems() {
        return displayItems;
    }

    @Override
    public Collection<ItemStack> getSearchTabDisplayItems() {
        return searchItems;
    }

    @Override
    public boolean contains(ItemStack stack) {
        return searchItems.contains(stack);
    }

    @Override
    public boolean hasAnyItems() {
        return !searchItems.isEmpty();
    }

    public List<SectionLayout> sectionLayouts() {
        return sectionLayouts;
    }

    public int visibleStartRow(float scrollOffset) {
        int hiddenRows = Math.max(Mth.positiveCeilDiv(displayItems.size(), COLUMNS) - VISIBLE_ROWS, 0);
        return Math.max((int) (scrollOffset * hiddenRows + 0.5F), 0);
    }

    private static void addEmptyRow(List<ItemStack> items) {
        for (int column = 0; column < COLUMNS; column++) {
            items.add(ItemStack.EMPTY);
        }
    }

    private static void padToCompleteRow(List<ItemStack> items) {
        int remainder = items.size() % COLUMNS;
        if (remainder == 0) {
            return;
        }
        for (int column = remainder; column < COLUMNS; column++) {
            items.add(ItemStack.EMPTY);
        }
    }

    public record SectionLayout(Component title, int headingRow) {
    }
}
