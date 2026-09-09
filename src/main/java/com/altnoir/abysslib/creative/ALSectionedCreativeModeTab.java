package com.altnoir.abysslib.creative;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 分区式创造栏（照 MIA-26.1 SectionedCreativeModeTab 移植）。
 *
 * <p>内容按分区划分为带标题、可滚动的区块；每个分区为其标题保留一整行物品格。
 * 客户端渲染器绘制该行，物品网格/滚动/搜索仍交给原版。</p>
 */
public final class ALSectionedCreativeModeTab extends CreativeModeTab {
    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 5;

    private final List<ALCreativeTabSection> sections;
    private Collection<ItemStack> displayItems = List.of();
    private Set<ItemStack> searchItems = ItemStackLinkedSet.createTypeAndComponentsSet();
    private List<SectionLayout> sectionLayouts = List.of();

    private ALSectionedCreativeModeTab(Builder builder, List<ALCreativeTabSection> sections) {
        super(builder);
        this.sections = List.copyOf(sections);
    }

    public static Builder configure(Builder builder, ALCreativeTabSection... sections) {
        List<ALCreativeTabSection> sectionList = List.of(sections);
        return builder.withTabFactory(tabBuilder -> new ALSectionedCreativeModeTab(tabBuilder, sectionList));
    }

    @Override
    public void buildContents(ItemDisplayParameters parameters) {
        List<ItemStack> newDisplayItems = new ArrayList<>();
        Set<ItemStack> newSearchItems = ItemStackLinkedSet.createTypeAndComponentsSet();
        Set<ItemStack> seenDisplayItems = ItemStackLinkedSet.createTypeAndComponentsSet();
        List<SectionLayout> newLayouts = new ArrayList<>();

        for (ALCreativeTabSection section : sections) {
            List<ItemStack> enabledItems = new ArrayList<>();
            for (ItemStack stack : section.itemStacks()) {
                if (stack.getItem().isEnabled(parameters.enabledFeatures()) && seenDisplayItems.add(stack)) {
                    enabledItems.add(stack);
                    newSearchItems.add(stack);
                }
            }

            if (enabledItems.isEmpty()) {
                continue;
            }

            int headingRow = newDisplayItems.size() / COLUMNS;
            newLayouts.add(new SectionLayout(section.title(), section.bannerSprite().orElse(null), headingRow));
            addEmptyRow(newDisplayItems);
            newDisplayItems.addAll(enabledItems);
            padToCompleteRow(newDisplayItems);
        }

        displayItems = List.copyOf(newDisplayItems);
        searchItems = newSearchItems;
        sectionLayouts = List.copyOf(newLayouts);
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

    public record SectionLayout(Component title, @Nullable Identifier bannerSprite, int headingRow) {
    }
}
