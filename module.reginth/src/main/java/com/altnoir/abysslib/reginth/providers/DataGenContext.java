package com.altnoir.abysslib.reginth.providers;

import com.altnoir.abysslib.reginth.builders.Builder;
import com.altnoir.abysslib.reginth.util.nullness.NonNullSupplier;
import com.altnoir.abysslib.reginth.util.nullness.NonnullType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * A context bean passed to data generator callbacks. Contains the entry that data is being created for, and some metadata about the entry.
 *
 * @param <R>
 *            Type of the registry to which the entry belongs
 * @param <E>
 *            Type of the object for which data is being generated
 */
public final class DataGenContext<R, E extends R> implements NonNullSupplier<E> {
    private final NonNullSupplier<E> entry;
    private final String name;
    private final ResourceLocation id;

    @SuppressWarnings("null")
    @NonnullType
    public E getEntry() {
        return entry.get();
    }

    @Deprecated
    public static <R, E extends R> DataGenContext<R, E> from(Builder<R, E, ?, ?> builder, ResourceKey<? extends Registry<R>> type) {
        return from(builder);
    }

    public static <R, E extends R> DataGenContext<R, E> from(Builder<R, E, ?, ?> builder) {
        return new DataGenContext<>(NonNullSupplier.of(builder.getOwner().get(builder.getName(), builder.getRegistryKey())), builder.getName(), ResourceLocation.fromNamespaceAndPath(builder.getOwner().getModid(), builder.getName()));
    }

    public DataGenContext(final NonNullSupplier<E> entry, final String name, final ResourceLocation id) {
        this.entry = entry;
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof DataGenContext<?, ?> other)) return false;
        final Object this$entry = this.getEntry();
        final Object other$entry = other.getEntry();
        if (!Objects.equals(this$entry, other$entry)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (!Objects.equals(this$name, other$name)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        return Objects.equals(this$id, other$id);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $entry = this.getEntry();
        result = result * PRIME + ($entry == null ? 43 : $entry.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "DataGenContext(entry=" + this.getEntry() + ", name=" + this.getName() + ", id=" + this.getId() + ")";
    }

    public E get() {
        return this.entry.get();
    }

    public NonNullSupplier<E> lazy() {
        return this.entry.lazy();
    }
}
