package com.altnoir.abysslib.creative;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 创造栏分区：一个标题 + 一组惰性物品条目。
 * 供 {@link ALSectionedCreativeModeTab} 使用；物品经 {@link #add} 加入后，
 * 标签页构建时会按分区顺序渲染（分区之间以空行分隔）。
 */
public final class ALCreativeTabSection {
    private final String translationKey;
    private final Component title;
    private final List<Supplier<ItemStack>> entries = new ArrayList<>();

    public ALCreativeTabSection(String translationKey) {
        this.translationKey = translationKey;
        this.title = Component.translatable(translationKey);
    }

    public String translationKey() {
        return translationKey;
    }

    public Component title() {
        return title;
    }

    public void add(ItemLike item) {
        add(() -> item.asItem().getDefaultInstance());
    }

    public void add(ItemStack stack) {
        add(stack::copy);
    }

    public void add(Supplier<ItemStack> stack) {
        entries.add(stack);
    }

    public void clear() {
        entries.clear();
    }

    public List<ItemStack> itemStacks() {
        return entries.stream()
                .map(Supplier::get)
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }
}
