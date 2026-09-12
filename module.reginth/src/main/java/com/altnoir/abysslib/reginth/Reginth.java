package com.altnoir.abysslib.reginth;

import com.altnoir.abysslib.creative.ALCreativeTabSection;
import com.altnoir.abysslib.reginth.builders.BlockBuilder;
import com.altnoir.abysslib.reginth.builders.ItemBuilder;
import com.altnoir.abysslib.reginth.builders.NoConfigBuilder;
import com.altnoir.abysslib.reginth.builders.ReginthBlockBuilder;
import com.altnoir.abysslib.reginth.builders.ReginthItemBuilder;
import com.altnoir.abysslib.reginth.providers.ReginthLangProvider;
import com.altnoir.abysslib.reginth.util.entry.ItemEntry;
import com.altnoir.abysslib.reginth.util.nullness.NonNullFunction;
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
 * <b>AbyssLib 内置注册框架的入口</b>（源码级内置自上游 Registrate，包名与类名已改为本库命名空间）。
 *
 * <p>除上游能力外，本类额外承接**分区式创造栏**的接线：通过 {@link #defaultCreativeSection}
 * 设定默认分区后，之后经 {@link #block} / {@link #item} 注册的方块与物品会自动归入该分区
 * （用 {@link #ignore()}（builder 上）排除）。
 *
 * <p>用法：每个模组入口持有绑定自己 modid 的一个实例：
 * <pre>{@code
 * private static final Reginth REGINTH = Reginth.create(MOD_ID);
 *
 * public MyMod(IEventBus modEventBus) {
 *     REGINTH.registerEventListeners(modEventBus);   // 手动挂载（重要）
 *     MyItems.register();
 *     MyBlocks.register();
 * }
 * }</pre>
 *
 * <p><b>与 1.21.1 线的差异</b>：26.1 线的 {@link #create} <b>不会</b>自动挂事件总线
 * （上游与 1.21.1 线会在 {@code create} 里查 {@code ModList} 自动挂载）；本线保持"入口自行调用
 * {@link #registerEventListeners} "的既有语义，不要两处都挂（会重复注册）。
 */
public class Reginth extends AbstractReginth<Reginth> {

    private final Set<String> ignoredCreativeTabEntries = new HashSet<>();
    private ResourceKey<CreativeModeTab> defaultCreativeTab;
    private ALCreativeTabSection defaultCreativeSection;

    protected Reginth(String modId) {
        super(modId);
    }

    public static Reginth create(String modId) {
        return new Reginth(modId);
    }

    @Override
    public Reginth defaultCreativeTab(ResourceKey<CreativeModeTab> creativeModeTab) {
        defaultCreativeTab = creativeModeTab;
        defaultCreativeSection = null;
        return super.defaultCreativeTab(creativeModeTab);
    }

    public Reginth defaultCreativeSection(ALCreativeTabSection section) {
        defaultCreativeTab = section.tab();
        defaultCreativeSection = section;
        return super.defaultCreativeTab(section.tab());
    }

    @Override
    public <T extends Item, P> ReginthItemBuilder<T, P> item(
            P parent,
            String name,
            NonNullFunction<Item.Properties, T> factory
    ) {
        return (ReginthItemBuilder<T, P>) this
                .<Item, T, P, ItemBuilder<T, P>>entry(name, callback -> {
                    ReginthItemBuilder<T, P> builder =
                            ReginthItemBuilder.create(this, parent, name, callback, factory);
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
    public <T extends Item> ReginthItemBuilder<T, Reginth> item(
            NonNullFunction<Item.Properties, T> factory
    ) {
        return item(self(), currentName(), factory);
    }

    @Override
    public <T extends Item> ReginthItemBuilder<T, Reginth> item(
            String name,
            NonNullFunction<Item.Properties, T> factory
    ) {
        return item(self(), name, factory);
    }

    @Override
    public <T extends Item, P> ReginthItemBuilder<T, P> item(
            P parent,
            NonNullFunction<Item.Properties, T> factory
    ) {
        return item(parent, currentName(), factory);
    }

    @Override
    public <T extends Block, P> ReginthBlockBuilder<T, P> block(
            P parent,
            String name,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return (ReginthBlockBuilder<T, P>) this
                .<Block, T, P, BlockBuilder<T, P>>entry(
                        name,
                        callback -> ReginthBlockBuilder.create(this, parent, name, callback, factory)
                );
    }

    @Override
    public <T extends Block> ReginthBlockBuilder<T, Reginth> block(
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return block(self(), currentName(), factory);
    }

    @Override
    public <T extends Block> ReginthBlockBuilder<T, Reginth> block(
            String name,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return block(self(), name, factory);
    }

    @Override
    public <T extends Block, P> ReginthBlockBuilder<T, P> block(
            P parent,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return block(parent, currentName(), factory);
    }

    /** 把某个注册名从默认创造栏（及其分区）剔除；由 builder 的 {@code ignore()} 调用。 */
    public void ignoreCreativeTab(String name) {
        ignoredCreativeTabEntries.add(name);
    }

    /** {@return 当前默认创造栏的 key（没设过则为空）}；由 builder 的 {@code ignore()} 调用。 */
    public Optional<ResourceKey<CreativeModeTab>> defaultCreativeTabKey() {
        return Optional.ofNullable(defaultCreativeTab);
    }

    public NoConfigBuilder<CreativeModeTab, CreativeModeTab, Reginth> creativeTab(Consumer<CreativeModeTab.Builder> config) {
        return creativeTab(self(), config);
    }

    public NoConfigBuilder<CreativeModeTab, CreativeModeTab, Reginth> creativeTab(String name) {
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
                    .title(this.addLang("itemGroup", modResource(name), ReginthLangProvider.toEnglishName(name)));
            config.accept(builder);
            return builder.build();
        });
    }

    private Identifier modResource(String path) {
        return Identifier.fromNamespaceAndPath(getModid(), path);
    }
}
