package com.altnoir.abysslib.registrate;

import com.altnoir.abysslib.creative.ALCreativeTabSection;
import com.mojang.logging.LogUtils;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 通用 Registrate 实例（对照 PoopSky 的 PoRegistrate 抽象，去掉 PoopSky 特有内容）。
 * <p>
 * 用法：模组入口持有一个实例：
 * <pre>{@code
 * private static final ALRegistrate REGISTRATE = ALRegistrate.create(MOD_ID);
 * }</pre>
 * 通过 {@link #defaultCreativeSection} 设置后，后续经 {@code block()}/{@code item()}
 * 注册的方块/物品会自动归入对应创造栏分区（忽略列表中的除外）。
 */
public class ALRegistrate extends AbstractRegistrate<ALRegistrate> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ALCreativeTabSection defaultCreativeSection;
    private final Set<String> ignoredCreativeTabEntries = new HashSet<>();

    protected ALRegistrate(String modid) {
        super(modid);
    }

    public static ALRegistrate create(String modId) {
        ALRegistrate registrate = new ALRegistrate(modId);
        Optional<IEventBus> modEventBus = ModList.get()
                .getModContainerById(modId)
                .map(ModContainer::getEventBus);
        modEventBus.ifPresentOrElse(registrate::registerEventListeners,
                () -> LOGGER.error("Failed to register event listeners for mod {}", modId));
        return registrate;
    }

    public ALRegistrate defaultCreativeSection(ALCreativeTabSection section) {
        this.defaultCreativeSection = section;
        return this;
    }

    void ignoreCreativeTab(String name) {
        ignoredCreativeTabEntries.add(name);
    }

    /**
     * 判断某注册名是否被显式排除在默认创造栏分区之外。
     * 包内可见：{@link ALItemBuilder#ignore()} / {@link ALBlockBuilder#ignore()} 通过它
     * 在 register 阶段二次校验，保证链式调用中途调用 ignore() 也生效。
     */
    boolean isIgnoredCreativeTab(String name) {
        return ignoredCreativeTabEntries.contains(name);
    }

    @Override
    public <T extends Block, P> ALBlockBuilder<T, P> block(
            P parent,
            String name,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return (ALBlockBuilder<T, P>) this
                .entry(name, callback -> {
                    ALBlockBuilder<T, P> builder = ALBlockBuilder.create(this, parent, name, callback, factory);
                    if (defaultCreativeSection != null && !ignoredCreativeTabEntries.contains(name)) {
                        builder.defaultCreativeSection(defaultCreativeSection);
                    }
                    return builder;
                });
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

    @Override
    public <T extends Item, P> ALItemBuilder<T, P> item(
            P parent,
            String name,
            NonNullFunction<Item.Properties, T> factory
    ) {
        return (ALItemBuilder<T, P>) this
                .entry(name, callback -> {
                    ALItemBuilder<T, P> builder = ALItemBuilder.create(this, parent, name, callback, factory);
                    if (defaultCreativeSection != null && !ignoredCreativeTabEntries.contains(name)) {
                        builder.defaultCreativeSection(defaultCreativeSection);
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
}
