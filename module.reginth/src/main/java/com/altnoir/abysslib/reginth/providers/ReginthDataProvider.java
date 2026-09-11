package com.altnoir.abysslib.reginth.providers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.altnoir.abysslib.reginth.AbstractReginth;
import com.altnoir.abysslib.reginth.util.DebugMarkers;
import com.altnoir.abysslib.reginth.util.nullness.NonnullType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReginthDataProvider implements DataProvider {
	private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ReginthDataProvider.class);
	@SuppressWarnings("null")
	static final BiMap<String, ProviderType<?>> TYPES = HashBiMap.create();
	static final Map<ResourceKey<? extends Registry<?>>, ProviderType<?>> TAG_TYPES = new ConcurrentHashMap<>();

	@Nullable
	public static String getTypeName(ProviderType<?> type) {
		return TYPES.inverse().get(type);
	}

	private final String mod;
	private final Map<ProviderType<?>, ReginthProvider> subProviders = new LinkedHashMap<>();
	private final CompletableFuture<HolderLookup.Provider> registriesLookup;

	public ReginthDataProvider(AbstractReginth<?> parent, String modid, GatherDataEvent event) {
		this.mod = modid;
		this.registriesLookup = event.getLookupProvider();
		EnumSet<LogicalSide> sides = EnumSet.noneOf(LogicalSide.class);
		if (event.includeServer()) {
			sides.add(LogicalSide.SERVER);
		}
		if (event.includeClient()) {
			sides.add(LogicalSide.CLIENT);
		}
		log.debug(DebugMarkers.DATA, "Gathering providers for sides: {}", sides);
		Map<ProviderType<?>, ReginthProvider> known = new HashMap<>();
		for (DataProviderInitializer.Sorted sorted : parent.getDataGenInitializer().getSortedProviders()) {
			ProviderType<?> type = sorted.type();
			var lookup = registriesLookup;
			if (sorted.parent() != null) lookup = ((ReginthLookupFillerProvider) known.get(sorted.parent())).getFilledProvider();
			ReginthProvider prov = ProviderType.create(type, parent, event, known, lookup);
			if (prov instanceof ReginthTagsProvider<?> tagsProvider && TAG_TYPES.get(tagsProvider.registry()) != type) {
				throw new IllegalStateException("Tag providers must be registered through ProviderType::registerTag");
			}
			known.put(type, prov);
			if (sides.contains(prov.getSide())) {
				log.debug(DebugMarkers.DATA, "Adding provider for type: {}", sorted.id());
				subProviders.put(type, prov);
			}
		}
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return registriesLookup.thenCompose(provider -> {
			var list = Lists.<CompletableFuture<?>>newArrayList();
			for (Map.Entry<@NonnullType ProviderType<?>, ReginthProvider> e : subProviders.entrySet()) {
				log.debug(DebugMarkers.DATA, "Generating data for type: {}", getTypeName(e.getKey()));
				list.add(e.getValue().run(cache));
			}
			;
			return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
		});
	}

	@Override
	public String getName() {
		return "Reginth Provider for " + mod + " [" + subProviders.values().stream().map(DataProvider::getName).collect(Collectors.joining(", ")) + "]";
	}

	@SuppressWarnings("unchecked")
	public <P extends ReginthProvider> Optional<P> getSubProvider(ProviderType<P> type) {
		return Optional.ofNullable((P) subProviders.get(type));
	}
}
