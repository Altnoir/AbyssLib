package com.altnoir.abysslib.reginth.builders;

import com.altnoir.abysslib.reginth.Reginth;
import com.altnoir.abysslib.reginth.util.nullness.NonNullFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 方块构建器：在 {@link BlockBuilder} 之上默认自动生成 blockstate / 战利品表 / 语言键。
 *
 * <p>由 {@link Reginth#block} 返回。与上游 {@code BlockBuilder} 的差别就是上面那件默认行为，
 * 以及 {@link #ignore()}（从默认创造栏剔除该方块的物品）。
 */
public class ReginthBlockBuilder<T extends Block, P> extends BlockBuilder<T, P> {

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

    /** 将该方块的物品从当前默认创造栏（及其分区）剔除。 */
    public ReginthBlockBuilder<T, P> ignore() {
        getOwner().ignoreCreativeTab(getName());
        return this;
    }

    @Override
    public Reginth getOwner() {
        return (Reginth) super.getOwner();
    }
}
