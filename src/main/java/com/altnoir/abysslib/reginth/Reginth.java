package com.altnoir.abysslib.reginth;

import com.altnoir.abysslib.creative.ALCreativeTabSection;
import com.altnoir.abysslib.reginth.builders.ReginthBlockBuilder;
import com.altnoir.abysslib.reginth.builders.ReginthItemBuilder;
import com.altnoir.abysslib.reginth.util.nullness.NonNullFunction;
import com.mojang.logging.LogUtils;
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
 * AbyssLib 内置的注册框架入口（源码级内置自上游 Registrate，包名与类名已改为本库命名空间）。
 * <p>
 * 除上游能力外，本类额外承担**分区式创造栏**的接线：通过 {@link #defaultCreativeSection}
 * 设定默认分区后，之后经 {@link #block}/{@link #item} 注册的方块/物品会自动归入该分区
 * （用 {@code ignore()} 排除，或用 {@code addTabSection(..)} 追加到其它分区）。
 * <p>
 * 用法：模组入口持有一个实例：
 * <pre>{@code
 * private static final Reginth REGINTH = Reginth.create(MOD_ID);
 * }</pre>
 */
public class Reginth extends AbstractReginth<Reginth> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ALCreativeTabSection defaultCreativeSection;
    private final Set<String> ignoredCreativeTabEntries = new HashSet<>();

    protected Reginth(String modid) {
        super(modid);
    }

    /**
     * 创建一个绑定 {@code modid} 的 {@link Reginth}，并为其注册事件监听器
     * （注册与数据生成）。与上游语义一致。
     */
    public static Reginth create(String modId) {
        Reginth reginth = new Reginth(modId);
        Optional<IEventBus> modEventBus = ModList.get()
                .getModContainerById(modId)
                .map(ModContainer::getEventBus);
        modEventBus.ifPresentOrElse(reginth::registerEventListeners,
                () -> LOGGER.error("Failed to register event listeners for mod {}", modId));
        return reginth;
    }

    /** 设定默认创造栏分区：之后注册的方块/物品自动加入该分区。 */
    public Reginth defaultCreativeSection(ALCreativeTabSection section) {
        this.defaultCreativeSection = section;
        return this;
    }

    /**
     * 把某注册名加入创造栏分区排除表。
     * 供 {@code ReginthBlockBuilder}/{@code ReginthItemBuilder} 的 {@code ignore()} 调用。
     */
    public void ignoreCreativeTab(String name) {
        ignoredCreativeTabEntries.add(name);
    }

    /**
     * 判断某注册名是否被显式排除在默认创造栏分区之外。
     * {@link ReginthItemBuilder#ignore()} / {@link ReginthBlockBuilder#ignore()} 通过它
     * 在 register 阶段二次校验，保证链式调用中途调用 ignore() 也生效。
     */
    public boolean isIgnoredCreativeTab(String name) {
        return ignoredCreativeTabEntries.contains(name);
    }

    @Override
    public <T extends Block, P> ReginthBlockBuilder<T, P> block(
            P parent,
            String name,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return (ReginthBlockBuilder<T, P>) this
                .entry(name, callback -> {
                    ReginthBlockBuilder<T, P> builder = ReginthBlockBuilder.create(this, parent, name, callback, factory);
                    if (defaultCreativeSection != null && !ignoredCreativeTabEntries.contains(name)) {
                        builder.defaultCreativeSection(defaultCreativeSection);
                    }
                    return builder;
                });
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

    @Override
    public <T extends Item, P> ReginthItemBuilder<T, P> item(
            P parent,
            String name,
            NonNullFunction<Item.Properties, T> factory
    ) {
        return (ReginthItemBuilder<T, P>) this
                .entry(name, callback -> {
                    ReginthItemBuilder<T, P> builder = ReginthItemBuilder.create(this, parent, name, callback, factory);
                    if (defaultCreativeSection != null && !ignoredCreativeTabEntries.contains(name)) {
                        builder.defaultCreativeSection(defaultCreativeSection);
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
}
