package com.altnoir.abysslib.reginth.builders;

import com.altnoir.abysslib.creative.ALCreativeTabSection;
import com.altnoir.abysslib.reginth.Reginth;
import com.altnoir.abysslib.reginth.util.entry.BlockEntry;
import com.altnoir.abysslib.reginth.util.nullness.NonNullFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块构建器：在 {@link BlockBuilder} 之上默认自动生成 blockstate / 战利品表 / 语言键，
 * 注册后自动加入创造栏分区。
 * <p>
 * 由 {@link Reginth#block} 返回。与上游 {@code BlockBuilder} 的区别就是上述两件默认行为，
 * 以及分区创造栏支持（{@link #ignore()} / {@link #addTabSection}）。
 */
public class ReginthBlockBuilder<T extends Block, P> extends BlockBuilder<T, P> {
    private ALCreativeTabSection defaultCreativeSection;
    private final List<ALCreativeTabSection> extraSections = new ArrayList<>();

    protected ReginthBlockBuilder(
            Reginth owner,
            P parent,
            String name,
            BuilderCallback callback,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        super(owner, parent, name, callback, factory, BlockBehaviour.Properties::of);
    }

    public static <T extends Block, P> ReginthBlockBuilder<T, P> create(
            Reginth owner,
            P parent,
            String name,
            BuilderCallback callback,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        ReginthBlockBuilder<T, P> builder = new ReginthBlockBuilder<>(owner, parent, name, callback, factory);
        builder.defaultBlockstate().defaultLoot().defaultLang();
        return builder;
    }

    public void defaultCreativeSection(ALCreativeTabSection section) {
        this.defaultCreativeSection = section;
    }

    /** 将该方块从当前默认创造栏分区中排除（链式调用中任意位置调用均生效）。 */
    public ReginthBlockBuilder<T, P> ignore() {
        getOwner().ignoreCreativeTab(getName());
        return this;
    }

    /**
     * 额外加入一个创造栏分区（在默认分区之外；可多次调用加入多个分区）。
     * 方块对应的方块物品会在注册完成后加入该分区。
     */
    public ReginthBlockBuilder<T, P> addTabSection(ALCreativeTabSection section) {
        extraSections.add(section);
        return this;
    }

    @Override
    public BlockEntry<T> register() {
        BlockEntry<T> entry = super.register();
        if (defaultCreativeSection != null && !getOwner().isIgnoredCreativeTab(getName())) {
            defaultCreativeSection.add(() -> entry.get().asItem().getDefaultInstance());
        }
        for (ALCreativeTabSection section : extraSections) {
            section.add(() -> entry.get().asItem().getDefaultInstance());
        }
        return entry;
    }

    @Override
    public Reginth getOwner() {
        return (Reginth) super.getOwner();
    }
}
