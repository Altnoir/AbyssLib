package com.altnoir.abysslib.registrate;

import com.altnoir.abysslib.creative.ALCreativeTabSection;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.builders.NoConfigBuilder;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 通用 Registrate 实例（照 MIA-26.1 MiaRegistrate 移植）。
 *
 * <p>与旧 ALRegistrate 的差异：分区条目按注册名收集、item 自动挂到默认创造栏的 ResourceKey
 * 上（{@code builder.tab(...)}），并自动加进默认 {@link ALCreativeTabSection}；block 不做自动归类
 * （其方块物品经 builder 的 item 链触发同一逻辑）。入口类自行挂事件总线：
 * {@code REGISTRATE.registerEventListeners(modEventBus);}（不再由 create 自动挂载）。</p>
 */
public class ALRegistrate extends AbstractRegistrate<ALRegistrate> {
    private final Set<String> ignoredCreativeTabEntries = new HashSet<>();
    private ResourceKey<CreativeModeTab> defaultCreativeTab;
    private ALCreativeTabSection defaultCreativeSection;

    protected ALRegistrate(String modId) {
        super(modId);
    }

    public static ALRegistrate create(String modId) {
        return new ALRegistrate(modId);
    }

    @Override
    public ALRegistrate defaultCreativeTab(ResourceKey<CreativeModeTab> creativeModeTab) {
        defaultCreativeTab = creativeModeTab;
        defaultCreativeSection = null;
        return super.defaultCreativeTab(creativeModeTab);
    }

    public ALRegistrate defaultCreativeSection(ALCreativeTabSection section) {
        defaultCreativeTab = section.tab();
        defaultCreativeSection = section;
        return super.defaultCreativeTab(section.tab());
    }

    @Override
    public <T extends Item, P> ALItemBuilder<T, P> item(
            P parent,
            String name,
            NonNullFunction<Item.Properties, T> factory
    ) {
        return (ALItemBuilder<T, P>) this
                .<Item, T, P, ItemBuilder<T, P>>entry(name, callback -> {
                    ALItemBuilder<T, P> builder =
                            ALItemBuilder.create(this, parent, name, callback, factory);
                    if (defaultCreativeTab != null && !ignoredCreativeTabEntries.contains(name)) {
                        builder.tab(defaultCreativeTab);
                        if (defaultCreativeSection != null) {
                            defaultCreativeSection.add(modResource(name));
                        }
                    }
                    return builder;
                });
    }

    @Override
    public <T extends Item> ALItemBuilder<T, ALRegistrate> item(
            NonNullFunction<Item.Properties, T> factory
    ) {
        return item(self(), currentName(), factory);
    }

    @Override
    public <T extends Item> ALItemBuilder<T, ALRegistrate> item(
            String name,
            NonNullFunction<Item.Properties, T> factory
    ) {
        return item(self(), name, factory);
    }

    @Override
    public <T extends Item, P> ALItemBuilder<T, P> item(
            P parent,
            NonNullFunction<Item.Properties, T> factory
    ) {
        return item(parent, currentName(), factory);
    }

    @Override
    public <T extends Block, P> ALBlockBuilder<T, P> block(
            P parent,
            String name,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return (ALBlockBuilder<T, P>) this
                .<Block, T, P, BlockBuilder<T, P>>entry(
                        name,
                        callback -> ALBlockBuilder.create(this, parent, name, callback, factory)
                );
    }

    @Override
    public <T extends Block> ALBlockBuilder<T, ALRegistrate> block(
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return block(self(), currentName(), factory);
    }

    @Override
    public <T extends Block> ALBlockBuilder<T, ALRegistrate> block(
            String name,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return block(self(), name, factory);
    }

    @Override
    public <T extends Block, P> ALBlockBuilder<T, P> block(
            P parent,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return block(parent, currentName(), factory);
    }

    void ignoreCreativeTab(String name) {
        ignoredCreativeTabEntries.add(name);
    }

    Optional<ResourceKey<CreativeModeTab>> defaultCreativeTabKey() {
        return Optional.ofNullable(defaultCreativeTab);
    }

    public NoConfigBuilder<CreativeModeTab, CreativeModeTab, ALRegistrate> creativeTab(Consumer<CreativeModeTab.Builder> config) {
        return creativeTab(self(), config);
    }

    public NoConfigBuilder<CreativeModeTab, CreativeModeTab, ALRegistrate> creativeTab(String name) {
        return creativeTab(self(), name);
    }

    public <P> NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> creativeTab(P parent, Consumer<CreativeModeTab.Builder> config) {
        return creativeTab(parent, currentName(), config);
    }

    public <P> NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> creativeTab(P parent, String name) {
        return creativeTab(parent, name, tab -> {
        });
    }

    public <P> NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> creativeTab(P parent, String name, Consumer<CreativeModeTab.Builder> config) {
        return this.generic(parent, name, Registries.CREATIVE_MODE_TAB, () -> {
            var builder = CreativeModeTab.builder()
                    .icon(() -> getAll(Registries.ITEM).stream().findFirst().map(ItemEntry::cast).map(ItemEntry::asStack).orElse(new ItemStack(Items.AIR)))
                    .title(this.addLang("itemGroup", modResource(name), RegistrateLangProvider.toEnglishName(name)));
            config.accept(builder);
            return builder.build();
        });
    }

    private Identifier modResource(String path) {
        return Identifier.fromNamespaceAndPath(getModid(), path);
    }
}
