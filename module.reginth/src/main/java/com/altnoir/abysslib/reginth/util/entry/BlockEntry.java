package com.altnoir.abysslib.reginth.util.entry;

import com.altnoir.abysslib.reginth.AbstractReginth;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BlockEntry<T extends Block> extends ItemProviderEntry<Block, T> {

    public BlockEntry(AbstractReginth<?> owner, DeferredHolder<Block, T> delegate) {
        super(owner, delegate);
    }

    public BlockState getDefaultState() {
        return get().defaultBlockState();
    }

    public boolean has(BlockState state) {
        return is(state.getBlock());
    }
    
    public static <T extends Block> BlockEntry<T> cast(RegistryEntry<Block, T> entry) {
        return RegistryEntry.cast(BlockEntry.class, entry);
    }
}
