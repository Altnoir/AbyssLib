package com.altnoir.abysslib.registrate;

import com.altnoir.abysslib.creative.ALCreativeTabSection;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块构建器：默认自动生成 blockstate / 战利品表 / 语言键，注册后自动加入创造栏分区。
 */
public class ALBlockBuilder<T extends Block, P> extends BlockBuilder<T, P> {
    private ALCreativeTabSection defaultCreativeSection;
    private final List<ALCreativeTabSection> extraSections = new ArrayList<>();

    protected ALBlockBuilder(
            ALRegistrate owner,
            P parent,
            String name,
            BuilderCallback callback,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        super(owner, parent, name, callback, factory, BlockBehaviour.Properties::of);
    }

    static <T extends Block, P> ALBlockBuilder<T, P> create(
            ALRegistrate owner,
            P parent,
            String name,
            BuilderCallback callback,
            NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        ALBlockBuilder<T, P> builder = new ALBlockBuilder<>(owner, parent, name, callback, factory);
        builder.defaultBlockstate().defaultLoot().defaultLang();
        return builder;
    }

    void defaultCreativeSection(ALCreativeTabSection section) {
        this.defaultCreativeSection = section;
    }

    /** 将该方块从当前默认创造栏分区中排除（链式调用中任意位置调用均生效）。 */
    public ALBlockBuilder<T, P> ignore() {
        getOwner().ignoreCreativeTab(getName());
        return this;
    }

    /**
     * 额外加入一个创造栏分区（在默认分区之外；可多次调用加入多个分区）。
     * 方块对应的方块物品会在注册完成后加入该分区。
     */
    public ALBlockBuilder<T, P> addTabSection(ALCreativeTabSection section) {
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
    public ALRegistrate getOwner() {
        return (ALRegistrate) super.getOwner();
    }
}
