package com.altnoir.abysslib.reginth.builders;

import com.altnoir.abysslib.creative.ALCreativeTabSection;
import com.altnoir.abysslib.reginth.Reginth;
import com.altnoir.abysslib.reginth.util.entry.ItemEntry;
import com.altnoir.abysslib.reginth.util.nullness.NonNullFunction;
import net.minecraft.world.item.Item;

/**
 * 物品构建器：在 {@link ItemBuilder} 之上默认自动生成模型与语言键，注册后自动加入创造栏分区。
 * <p>
 * 由 {@link Reginth#item} 返回。
 */
public class ReginthItemBuilder<T extends Item, P> extends ItemBuilder<T, P> {
    private ALCreativeTabSection defaultCreativeSection;

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

    public void defaultCreativeSection(ALCreativeTabSection section) {
        this.defaultCreativeSection = section;
    }

    /** 将该物品从当前默认创造栏分区中排除（链式调用中任意位置调用均生效）。 */
    public ReginthItemBuilder<T, P> ignore() {
        getOwner().ignoreCreativeTab(getName());
        return this;
    }

    /**
     * 额外加入一个创造栏分区（在默认分区之外；可多次调用加入多个分区）。
     * 惰性求值：真正取值发生在创造栏内容构建时，因此 register 前调用即可。
     */
    public ReginthItemBuilder<T, P> addTabSection(ALCreativeTabSection section) {
        section.add(() -> getEntry().getDefaultInstance());
        return this;
    }

    @Override
    public ItemEntry<T> register() {
        ItemEntry<T> entry = super.register();
        if (defaultCreativeSection != null && !getOwner().isIgnoredCreativeTab(getName())) {
            defaultCreativeSection.add(() -> entry.get().getDefaultInstance());
        }
        return entry;
    }

    @Override
    public Reginth getOwner() {
        return (Reginth) super.getOwner();
    }
}
