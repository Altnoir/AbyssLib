package com.altnoir.abysslib.reginth.providers;

import javax.annotation.ParametersAreNonnullByDefault;

import com.altnoir.abysslib.reginth.AbstractReginth;
import com.altnoir.abysslib.reginth.providers.generators.*;
import com.altnoir.abysslib.reginth.providers.loot.ReginthLootTableProvider;
import com.altnoir.abysslib.reginth.util.nullness.NonNullSupplier;
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
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a type of data that can be generated, and specifies a factory for the provider.
 * <p>
 * Used as a key for data generator callbacks.
 * <p>
 * This file also defines the built-in provider types, but third-party types can be created with {@link #registerProvider(String, ProviderType)}.
 *
 * @param <T> The type of the provider
 */
@FunctionalInterface
@SuppressWarnings("deprecation")
@ParametersAreNonnullByDefault
public interface ProviderType<T extends ReginthProvider> extends GeneratorType<T> {

    // SERVER DATA
    ProviderType<ReginthDatapackProvider> DYNAMIC = registerServerData("dynamic", ReginthDatapackProvider::new);
    ProviderType<ReginthDataMapProvider> DATA_MAP = registerServerData("data_map", ReginthDataMapProvider::new);
    ProviderType<ReginthRecipeRunner> RECIPE_RUNNER = registerServerData("recipe_runner", ReginthRecipeRunner::new);
    ProviderType<ReginthAdvancementProvider> ADVANCEMENT = registerServerData("advancement", ReginthAdvancementProvider::new);
    ProviderType<ReginthLootTableProvider> LOOT = registerServerData("loot", ReginthLootTableProvider::new);
    ProviderType<ReginthTagsProvider.IntrinsicImpl<Block>> BLOCK_TAGS = registerIntrinsicTag("tags/block", "blocks", Registries.BLOCK, block -> block.builtInRegistryHolder().key());
    ProviderType<ReginthTagsProvider.Impl<Enchantment>> ENCHANTMENT_TAGS = registerDynamicTag("tags/enchantment", "enchantments", Registries.ENCHANTMENT);
    ProviderType<ReginthItemTagsProvider> ITEM_TAGS = registerTag("tags/item", Registries.ITEM, c -> new ReginthItemTagsProvider(c.parent(), c.type(), "items", c.output(), c.provider(), c.get(BLOCK_TAGS).contentsGetter()));
    ProviderType<ReginthTagsProvider.IntrinsicImpl<Fluid>> FLUID_TAGS = registerIntrinsicTag("tags/fluid", "fluids", Registries.FLUID, fluid -> fluid.builtInRegistryHolder().key());
    ProviderType<ReginthTagsProvider.IntrinsicImpl<EntityType<?>>> ENTITY_TAGS = registerIntrinsicTag("tags/entity", "entity_types", Registries.ENTITY_TYPE, entityType -> entityType.builtInRegistryHolder().key());
    ProviderType<ReginthGenericProvider> GENERIC_SERVER = registerProvider("Reginth_generic_server_provider",  c -> new ReginthGenericProvider(c.parent(), c.event(), LogicalSide.SERVER, c.type()));

    // CLIENT DATA
    ProviderType<ReginthModelProvider> MODEL = registerClientProvider("model", () -> c -> new ReginthModelProvider(c.parent(), c.output()));
    ProviderType<ReginthLangProvider> LANG = registerClientProvider("lang", () -> c -> new ReginthLangProvider(c.parent(), c.output()));
    ProviderType<ReginthGenericProvider> GENERIC_CLIENT = registerClientProvider("Reginth_generic_client_provider", () -> c -> new ReginthGenericProvider(c.parent(), c.event(), LogicalSide.CLIENT, c.type()));

    GeneratorType<ReginthRecipeProvider> RECIPE = RECIPE_RUNNER.createGenerator("recipe");
    GeneratorType<ReginthBlockModelGenerator> BLOCKSTATE = MODEL.createGenerator("blockstate");
    GeneratorType<ReginthItemModelGenerator> ITEM_MODEL = MODEL.createGenerator("item_model");

    record Context<T extends ReginthProvider>(ProviderType<T> type, AbstractReginth<?> parent,
                                                 @Deprecated GatherDataEvent event,
                                                 Map<ProviderType<?>, ReginthProvider> existing,
                                                 PackOutput output,
                                                 CompletableFuture<HolderLookup.Provider> provider) {

        @SuppressWarnings("unchecked")
        public <R extends ReginthProvider> R get(ProviderType<R> other) {
            return (R) existing().get(other);
        }

    }

    T create(Context<T> context);

    default <R> GeneratorType<R> createGenerator(String type) {
        return new GeneratorType<>() {
            public String toString(){
                return type;
            }
        };
    }

    interface SimpleServerDataFactory<T extends ReginthProvider> extends ProviderType<T> {

        T create(AbstractReginth<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> provider);

        @Override
        default T create(Context<T> context) {
            return create(context.parent(), context.output(), context.provider());
        }

        default ProviderType<T> asProvider() {
            return this;
        }

    }

    static <T extends ReginthProvider> ProviderType<T> registerServerData(String name, SimpleServerDataFactory<T> factory) {
        return registerProvider(name, factory.asProvider());
    }

    static <T extends ReginthProvider> ProviderType<T> registerProvider(String name, ProviderType<T> type) {
        ReginthDataProvider.TYPES.put(name, type);
        return type;
    }

    static <T extends ReginthProvider> ProviderType<T> registerClientProvider(String name, NonNullSupplier<ProviderType<T>> supplier) {
        if (!DatagenModLoader.isRunningDataGen()) return context -> null;
        var type = supplier.get();
        ReginthDataProvider.TYPES.put(name, type);
        return type;
    }

    @SuppressWarnings("unchecked")
    static <T, R extends ReginthTagsProvider<T>> ProviderType<R> registerTag(String name, ResourceKey<? extends Registry<T>> key, ProviderType<R> type) {
        if (ReginthDataProvider.TAG_TYPES.containsKey(key)) {
            return (ProviderType<R>) ReginthDataProvider.TAG_TYPES.get(key);
        }
        ReginthDataProvider.TAG_TYPES.put(key, type);
        ReginthDataProvider.TYPES.put(name, type);
        return type;
    }

    static <T> ProviderType<ReginthTagsProvider.IntrinsicImpl<T>> registerIntrinsicTag(String providerName, String typeName, ResourceKey<? extends Registry<T>> registry, Function<T, ResourceKey<T>> keyExtractor) {
        return registerTag(providerName, registry, c -> new ReginthTagsProvider.IntrinsicImpl<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider(), keyExtractor));
    }

    static <T> ProviderType<ReginthTagsProvider.Impl<T>> registerDynamicTag(String providerName, String typeName, ResourceKey<Registry<T>> registry) {
        return registerTag(providerName, registry, c -> new ReginthTagsProvider.Impl<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider()));
    }

    static <T extends ReginthProvider> T create(ProviderType<T> type, AbstractReginth<?> parent, GatherDataEvent event, Map<ProviderType<?>, ReginthProvider> existing, CompletableFuture<HolderLookup.Provider> provider) {
        return type.create(new Context<>(type, parent, event, existing, event.getGenerator().getPackOutput(), provider));
    }

}
