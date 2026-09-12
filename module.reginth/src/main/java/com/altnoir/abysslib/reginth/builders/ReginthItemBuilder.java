package com.altnoir.abysslib.reginth.builders;

import com.altnoir.abysslib.reginth.Reginth;
import com.altnoir.abysslib.reginth.util.nullness.NonNullFunction;
import net.minecraft.world.item.Item;

/**
 * 物品构建器：在 {@link ItemBuilder} 之上默认自动生成模型 / 语言键。
 *
 * <p>由 {@link Reginth#item} 返回。{@link #ignore()} 会把该物品从默认创造栏（及其分区）剔除。
 */
public class ReginthItemBuilder<T extends Item, P> extends ItemBuilder<T, P> {

    protected ReginthItemBuilder(
            Reginth owner,
            P parent,
            String name,
            BuilderCallback callback,
            NonNullFunction<Item.Properties, T> factory
    ) {
        super(owner, parent, name, callback, factory);
    }

    public static <T extends Item, P> ReginthItemBuilder<T, P> create(
            Reginth owner,
            P parent,
            String name,
            BuilderCallback callback,
            NonNullFunction<Item.Properties, T> factory
    ) {
        ReginthItemBuilder<T, P> builder = new ReginthItemBuilder<>(owner, parent, name, callback, factory);
        builder.defaultModel().defaultLang();
        return builder;
    }

    /** 将该物品从当前默认创造栏（及其分区）剔除。 */
    public ReginthItemBuilder<T, P> ignore() {
        getOwner().ignoreCreativeTab(getName());
        getOwner().defaultCreativeTabKey().ifPresent(this::removeTab);
        return this;
    }

    @Override
    public Reginth getOwner() {
        return (Reginth) super.getOwner();
    }
}
