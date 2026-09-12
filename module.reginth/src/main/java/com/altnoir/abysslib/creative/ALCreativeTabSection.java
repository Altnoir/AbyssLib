package com.altnoir.abysslib.creative;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 分区式创造栏里一个分区的注册目标（照 MIA-26.1 CreativeTabSection 移植）。
 *
 * <p>条目以注册名（Identifier）存储，因此 builder 可在物品注册完成前先把自己分进某区；
 * 展示时再经注册表惰性解析。</p>
 */
public final class ALCreativeTabSection {
    private final ResourceKey<CreativeModeTab> tab;
    private final Identifier id;
    private final Component title;
    private final @Nullable Identifier bannerSprite;
    private final Set<Identifier> itemIds = new LinkedHashSet<>();

    public ALCreativeTabSection(ResourceKey<CreativeModeTab> tab, Identifier id, Component title) {
        this(tab, id, title, null);
    }

    /**
     * 创建分区，可带可选的 162x18 GUI 横幅 sprite。
     *
     * <p>sprite id 遵循原版 GUI sprite 约定，指向
     * {@code assets/<namespace>/textures/gui/sprites/<path>.png}；不传时渲染器画默认横幅。</p>
     */
    public ALCreativeTabSection(
            ResourceKey<CreativeModeTab> tab,
            Identifier id,
            Component title,
            @Nullable Identifier bannerSprite
    ) {
        this.tab = tab;
        this.id = id;
        this.title = title;
        this.bannerSprite = bannerSprite;
    }

    public ResourceKey<CreativeModeTab> tab() {
        return tab;
    }

    public Identifier id() {
        return id;
    }

    public Component title() {
        return title;
    }

    public Optional<Identifier> bannerSprite() {
        return Optional.ofNullable(bannerSprite);
    }

    public void add(Identifier itemId) {
        itemIds.add(itemId);
    }

    public List<ItemStack> itemStacks() {
        return itemIds.stream()
                .map(BuiltInRegistries.ITEM::get)
                .flatMap(Optional::stream)
                .map(Holder.Reference::value)
                .filter(item -> item != Items.AIR)
                .map(Item::getDefaultInstance)
                .toList();
    }
}
