package com.altnoir.abysslib.registrate;

import com.altnoir.abysslib.creative.ALCreativeTabSection;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.world.item.Item;

/**
 * 物品构建器：默认自动生成模型与语言键，注册后自动加入创造栏分区。
 */
public class ALItemBuilder<T extends Item, P> extends ItemBuilder<T, P> {
    private ALCreativeTabSection defaultCreativeSection;

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

    void defaultCreativeSection(ALCreativeTabSection section) {
        this.defaultCreativeSection = section;
    }

    /** 将该物品从当前默认创造栏分区中排除（链式调用中任意位置调用均生效）。 */
    public ALItemBuilder<T, P> ignore() {
        getOwner().ignoreCreativeTab(getName());
        return this;
    }

    /**
     * 额外加入一个创造栏分区（在默认分区之外；可多次调用加入多个分区）。
     * 惰性求值：真正取值发生在创造栏内容构建时，因此 register 前调用即可。
     */
    public ALItemBuilder<T, P> addTabSection(ALCreativeTabSection section) {
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
    public ALRegistrate getOwner() {
        return (ALRegistrate) super.getOwner();
    }
}
