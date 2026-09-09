package com.altnoir.abysslib.registrate;

import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.world.item.Item;

/** Item builder（照 MIA-26.1 MiaItemBuilder 移植）：默认模型/语言键，支持从默认创造栏剔除。 */
public class ALItemBuilder<T extends Item, P> extends ItemBuilder<T, P> {
    protected ALItemBuilder(
            ALRegistrate owner,
            P parent,
            String name,
            BuilderCallback callback,
            NonNullFunction<Item.Properties, T> factory
    ) {
        super(owner, parent, name, callback, factory);
    }

    static <T extends Item, P> ALItemBuilder<T, P> create(
            ALRegistrate owner,
            P parent,
            String name,
            BuilderCallback callback,
            NonNullFunction<Item.Properties, T> factory
    ) {
        ALItemBuilder<T, P> builder = new ALItemBuilder<>(owner, parent, name, callback, factory);
        builder.defaultModel().defaultLang();
        return builder;
    }

    /** 将该物品从当前默认创造栏（及其分区）剔除。 */
    public ALItemBuilder<T, P> ignore() {
        getOwner().ignoreCreativeTab(getName());
        getOwner().defaultCreativeTabKey().ifPresent(this::removeTab);
        return this;
    }

    @Override
    public ALRegistrate getOwner() {
        return (ALRegistrate) super.getOwner();
    }
}
