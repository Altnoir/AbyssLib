package com.altnoir.abysslib.reginth.providers;

import com.altnoir.abysslib.reginth.AbstractReginth;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.DataMapProvider;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ReginthDataMapProvider extends DataMapProvider implements ReginthProvider {

	private final AbstractReginth<?> parent;

	private HolderLookup.@Nullable Provider provider;

	protected ReginthDataMapProvider(AbstractReginth<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> pvd) {
		super(output, pvd);
		this.parent = parent;
	}

	@Override
	public LogicalSide getSide() {
		return LogicalSide.SERVER;
	}

	/**
	 * Generate data map entries.
	 *
	 * @param provider
	 */
	@Override
	protected void gather(HolderLookup.Provider provider) {
		this.provider = provider;
		parent.genData(ProviderType.DATA_MAP, this);
		this.provider = null;
	}

	public HolderLookup.Provider getProvider() {
		if (provider == null) throw new IllegalStateException("Holder Lookup Provider is not available now");
		return provider;
	}

}
