package com.altnoir.abysslib.reginth.providers;

import com.altnoir.abysslib.reginth.AbstractReginth;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface ReginthTagsProvider<T> extends ReginthLookupFillerProvider {

    TagsProvider.TagAppender<T> addTag(TagKey<T> tag);

    CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter();

	ResourceKey<? extends Registry<T>> registry();

    class Impl<T> extends TagsProvider<T> implements ReginthTagsProvider<T> {
        private final AbstractReginth<?> owner;
        private final ProviderType<? extends Impl<T>> type;
        private final String name;

        public Impl(AbstractReginth<?> owner, ProviderType<? extends Impl<T>> type, String name, PackOutput packOutput, ResourceKey<? extends Registry<T>> registryIn, CompletableFuture<HolderLookup.Provider> registriesLookup, ExistingFileHelper existingFileHelper) {
            super(packOutput, registryIn, registriesLookup, owner.getModid(), existingFileHelper);

            this.owner = owner;
            this.type = type;
            this.name = name;
        }

        @Override
        public String getName() {
            return "Tags (%s)".formatted(name);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            owner.genData(type, this);
        }

        @Override
        public LogicalSide getSide() {
            return LogicalSide.SERVER;
        }

        @Override
        public TagAppender<T> addTag(TagKey<T> tag) {
            return super.tag(tag);
        }

        @Override
        public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
            return createContentsProvider();
        }

		@Override
		public ResourceKey<? extends Registry<T>> registry() {
			return registryKey;
		}

	}

    class IntrinsicImpl<T> extends IntrinsicHolderTagsProvider<T> implements ReginthTagsProvider<T> {
        private final AbstractReginth<?> owner;
        private final ProviderType<? extends IntrinsicImpl<T>> type;
        private final String name;

        public IntrinsicImpl(AbstractReginth<?> owner, ProviderType<? extends IntrinsicImpl<T>> type, String name, PackOutput packOutput, ResourceKey<? extends Registry<T>> registryIn, CompletableFuture<HolderLookup.Provider> registriesLookup, Function<T, ResourceKey<T>> keyExtractor, ExistingFileHelper existingFileHelper) {
            super(packOutput, registryIn, registriesLookup, keyExtractor, owner.getModid(), existingFileHelper);

            this.owner = owner;
            this.type = type;
            this.name = name;
        }

        @Override
        public String getName() {
            return "Tags (%s)".formatted(name);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            owner.genData(type, this);
        }

        @Override
        public LogicalSide getSide() {
            return LogicalSide.SERVER;
        }

        @Override
        public IntrinsicTagAppender<T> addTag(TagKey<T> tag) {
            return super.tag(tag);
        }

        @Override
        public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
            return createContentsProvider();
        }

		@Override
		public ResourceKey<? extends Registry<T>> registry() {
			return registryKey;
		}

	}
}
