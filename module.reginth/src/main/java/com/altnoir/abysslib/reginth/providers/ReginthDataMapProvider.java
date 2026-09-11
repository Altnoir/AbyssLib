package com.altnoir.abysslib.reginth.providers;

import com.altnoir.abysslib.reginth.AbstractReginth;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import java.util.concurrent.CompletableFuture;

public class ReginthDataMapProvider extends DataMapProvider implements ReginthProvider {

	private final AbstractReginth<?> parent;

	protected ReginthDataMapProvider(AbstractReginth<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> pvd) {
		super(output, pvd);
		this.parent = parent;
	}

	@Override
	public LogicalSide getSide() {
		return LogicalSide.SERVER;
	}

	@Override
	protected void gather() {
		parent.genData(ProviderType.DATA_MAP, this);
	}

}
