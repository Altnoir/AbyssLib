package com.altnoir.abysslib.reginth.providers;

import com.altnoir.abysslib.reginth.AbstractReginth;
import com.altnoir.abysslib.reginth.providers.loot.ReginthLootTableProvider;
import com.altnoir.abysslib.reginth.util.nullness.FieldsAreNonnullByDefault;
import com.altnoir.abysslib.reginth.util.nullness.NonNullBiFunction;
import com.altnoir.abysslib.reginth.util.nullness.NonNullFunction;
import com.altnoir.abysslib.reginth.util.nullness.NonNullUnaryOperator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a type of data that can be generated, and specifies a factory for the provider.
 * <p>
 * Used as a key for data generator callbacks.
 * <p>
 * This file also defines the built-in provider types, but third-party types can be created with {@link #register(String, ProviderType)}.
 *
 * @param <T> The type of the provider
 */
@FunctionalInterface
@SuppressWarnings("deprecation")
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public interface ProviderType<T extends ReginthProvider> {

    // SERVER DATA
    ProviderType<ReginthDatapackProvider> DYNAMIC = registerServerData("dynamic", ReginthDatapackProvider::new);
    ProviderType<ReginthDataMapProvider> DATA_MAP = registerServerData("data_map", ReginthDataMapProvider::new);
    ProviderType<ReginthRecipeProvider> RECIPE = registerServerData("recipe", ReginthRecipeProvider::new);
    ProviderType<ReginthAdvancementProvider> ADVANCEMENT = registerServerData("advancement", ReginthAdvancementProvider::new);
    ProviderType<ReginthLootTableProvider> LOOT = registerServerData("loot", ReginthLootTableProvider::new);
    ProviderType<ReginthTagsProvider.IntrinsicImpl<Block>> BLOCK_TAGS = registerIntrinsicTag("tags/block", "blocks", Registries.BLOCK, block -> block.builtInRegistryHolder().key());
    ProviderType<ReginthTagsProvider.Impl<Enchantment>> ENCHANTMENT_TAGS = registerDynamicTag("tags/enchantment", "enchantments", Registries.ENCHANTMENT);
    ProviderType<ReginthItemTagsProvider> ITEM_TAGS = registerTag("tags/item", Registries.ITEM, c -> new ReginthItemTagsProvider(c.parent(), c.type(), "items", c.output(), c.provider(), c.get(BLOCK_TAGS).contentsGetter(), c.fileHelper()));
    ProviderType<ReginthTagsProvider.IntrinsicImpl<Fluid>> FLUID_TAGS = registerIntrinsicTag("tags/fluid", "fluids", Registries.FLUID, fluid -> fluid.builtInRegistryHolder().key());
    ProviderType<ReginthTagsProvider.IntrinsicImpl<EntityType<?>>> ENTITY_TAGS = registerIntrinsicTag("tags/entity", "entity_types", Registries.ENTITY_TYPE, entityType -> entityType.builtInRegistryHolder().key());
    ProviderType<ReginthGenericProvider> GENERIC_SERVER = registerProvider("Reginth_generic_server_provider",  c -> new ReginthGenericProvider(c.parent(), c.event(), LogicalSide.SERVER, c.type()));

    // CLIENT DATA
    ProviderType<ReginthBlockstateProvider> BLOCKSTATE = registerProvider("blockstate", c -> new ReginthBlockstateProvider(c.parent(), c.output(), c.fileHelper()));
    ProviderType<ReginthItemModelProvider> ITEM_MODEL = registerProvider("item_model", c -> new ReginthItemModelProvider(c.parent(), c.output(), c.get(BLOCKSTATE).getExistingFileHelper()));
    ProviderType<ReginthLangProvider> LANG = registerProvider("lang", c -> new ReginthLangProvider(c.parent(), c.output()));
    ProviderType<ReginthGenericProvider> GENERIC_CLIENT = registerProvider("Reginth_generic_client_provider", c -> new ReginthGenericProvider(c.parent(), c.event(), LogicalSide.CLIENT, c.type()));

    record Context<T extends ReginthProvider>(ProviderType<T> type, AbstractReginth<?> parent,
                                                 @Deprecated GatherDataEvent event,
                                                 Map<ProviderType<?>, ReginthProvider> existing,
                                                 PackOutput output, ExistingFileHelper fileHelper,
                                                 CompletableFuture<HolderLookup.Provider> provider) {

        public <R extends ReginthProvider> R get(ProviderType<R> other) {
            return (R) existing().get(other);
        }

    }

    default T create(Context<T> context) {
        return create(context.parent(), context.event(), context.existing());
    }

    @Deprecated
    T create(AbstractReginth<?> parent, GatherDataEvent event, Map<ProviderType<?>, ReginthProvider> existing);

    interface DependencyAwareProviderType<T extends ReginthProvider> extends ProviderType<T> {

        @Override
        default T create(AbstractReginth<?> parent, GatherDataEvent event, Map<ProviderType<?>, ReginthProvider> existing) {
            return create(new Context<>(this, parent, event, existing, event.getGenerator().getPackOutput(), event.getExistingFileHelper(), event.getLookupProvider()));
        }

        @Override
        T create(Context<T> context);

    }

    interface SimpleServerDataFactory<T extends ReginthProvider> extends DependencyAwareProviderType<T> {

        T create(AbstractReginth<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> provider);

        @Override
        default T create(Context<T> context) {
            return create(context.parent(), context.output(), context.provider());
        }

        default ProviderType<T> asProvider() {
            return this;
        }

    }

    // TODO this is clunky af
    @Deprecated
    @Nonnull
    static <T extends ReginthProvider> ProviderType<T> registerDelegate(String name, NonNullUnaryOperator<ProviderType<T>> type) {
        ProviderType<T> ret = new ProviderType<T>() {

            @Override
            public T create(@Nonnull AbstractReginth<?> parent, GatherDataEvent event, Map<ProviderType<?>, ReginthProvider> existing) {
                return type.apply(this).create(parent, event, existing);
            }
        };
        return register(name, ret);
    }

    @Deprecated
    @Nonnull
    static <T extends ReginthProvider> ProviderType<T> register(String name, NonNullFunction<ProviderType<T>, NonNullBiFunction<AbstractReginth<?>, GatherDataEvent, T>> type) {
        ProviderType<T> ret = new ProviderType<T>() {

            @Override
            public T create(@Nonnull AbstractReginth<?> parent, GatherDataEvent event, Map<ProviderType<?>, ReginthProvider> existing) {
                return type.apply(this).apply(parent, event);
            }
        };
        return register(name, ret);
    }

    @Deprecated
    @Nonnull
    static <T extends ReginthProvider> ProviderType<T> register(String name, NonNullBiFunction<AbstractReginth<?>, GatherDataEvent, T> type) {
        ProviderType<T> ret = new ProviderType<T>() {

            @Override
            public T create(AbstractReginth<?> parent, GatherDataEvent event, Map<ProviderType<?>, ReginthProvider> existing) {
                return type.apply(parent, event);
            }
        };
        return register(name, ret);
    }

    @Deprecated
    @Nonnull
    static <T extends ReginthProvider> ProviderType<T> register(String name, ProviderType<T> type) {
        ReginthDataProvider.TYPES.put(name, type);
        return type;
    }

    @Nonnull
    static <T extends ReginthProvider> ProviderType<T> registerServerData(String name, SimpleServerDataFactory<T> factory) {
        return register(name, factory.asProvider());
    }

    @Nonnull
    static <T extends ReginthProvider> ProviderType<T> registerProvider(String name, DependencyAwareProviderType<T> type) {
        ReginthDataProvider.TYPES.put(name, type);
        return type;
    }

    @Nonnull
    static <T, R extends ReginthTagsProvider<T>> ProviderType<R> registerTag(String name, ResourceKey<? extends Registry<T>> key, DependencyAwareProviderType<R> type) {
        if (ReginthDataProvider.TAG_TYPES.containsKey(key)) {
            return (ProviderType<R>) ReginthDataProvider.TAG_TYPES.get(key);
        }
        ReginthDataProvider.TAG_TYPES.put(key, type);
        ReginthDataProvider.TYPES.put(name, type);
        return type;
    }

    @Nonnull
    static <T> ProviderType<ReginthTagsProvider.IntrinsicImpl<T>> registerIntrinsicTag(String providerName, String typeName, ResourceKey<? extends Registry<T>> registry, Function<T, ResourceKey<T>> keyExtractor) {
        return registerTag(providerName, registry, c -> new ReginthTagsProvider.IntrinsicImpl<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider(), keyExtractor, c.fileHelper()));
    }

    @Nonnull
    static <T> ProviderType<ReginthTagsProvider.Impl<T>> registerDynamicTag(String providerName, String typeName, ResourceKey<Registry<T>> registry) {
        return registerTag(providerName, registry, c -> new ReginthTagsProvider.Impl<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider(), c.fileHelper()));
    }

    static <T extends ReginthProvider> T create(ProviderType<T> type, AbstractReginth<?> parent, GatherDataEvent event, Map<ProviderType<?>, ReginthProvider> existing, CompletableFuture<HolderLookup.Provider> provider) {
        return type.create(new Context<>(type, parent, event, existing, event.getGenerator().getPackOutput(), event.getExistingFileHelper(), provider));
    }

}
