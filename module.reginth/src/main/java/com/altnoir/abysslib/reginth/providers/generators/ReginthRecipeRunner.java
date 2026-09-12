package com.altnoir.abysslib.reginth.providers.generators;

import com.altnoir.abysslib.reginth.AbstractReginth;
import com.altnoir.abysslib.reginth.providers.ReginthProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.fml.LogicalSide;

import java.util.concurrent.CompletableFuture;

public class ReginthRecipeRunner extends RecipeProvider.Runner implements ReginthProvider {

	final AbstractReginth<?> owner;

	@org.jetbrains.annotations.Nullable
	ReginthRecipeProvider provider;

	public ReginthRecipeRunner(AbstractReginth<?> owner, PackOutput p_365369_, CompletableFuture<HolderLookup.Provider> p_361563_) {
		super(p_365369_, p_361563_);
		this.owner = owner;
	}

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider p_362946_, RecipeOutput p_365274_) {
		return new ReginthRecipeProvider(this, p_362946_, p_365274_);
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public LogicalSide getSide() {
		return LogicalSide.SERVER;
	}

	public ReginthRecipeProvider getRecipeProvider() {
		if (provider == null) throw new IllegalStateException("Recipe Provider is not available now");
		return provider;
	}

}
