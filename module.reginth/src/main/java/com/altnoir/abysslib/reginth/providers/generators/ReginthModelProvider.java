package com.altnoir.abysslib.reginth.providers.generators;

import com.altnoir.abysslib.reginth.AbstractReginth;
import com.altnoir.abysslib.reginth.providers.ReginthProvider;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;

public class ReginthModelProvider extends ModelProvider implements ReginthProvider {

	private final AbstractReginth<?> parent;

	public ReginthModelProvider(AbstractReginth<?> parent, PackOutput p_388260_) {
		super(p_388260_, parent.getModid());
		this.parent = parent;
	}

	@Override
	protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
		new ReginthBlockModelGenerator(parent, blockModels.blockStateOutput, blockModels.itemModelOutput, blockModels.modelOutput).run();
		new ReginthItemModelGenerator(parent, itemModels.itemModelOutput, itemModels.modelOutput).run();
	}

	@Override
	public LogicalSide getSide() {
		return LogicalSide.CLIENT;
	}

}
