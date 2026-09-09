package com.altnoir.abysslib.registrate;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Block builder（照 MIA-26.1 MiaBlockBuilder 移植）：默认 blockstate/战利品/语言键。 */
public class ALBlockBuilder<T extends Block, P> extends BlockBuilder<T, P> {
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

    /** 将该方块的物品从当前默认创造栏剔除。 */
    public ALBlockBuilder<T, P> ignore() {
        getOwner().ignoreCreativeTab(getName());
        return this;
    }

    @Override
    public ALRegistrate getOwner() {
        return (ALRegistrate) super.getOwner();
    }
}
