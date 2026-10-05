package com.altnoir.abysslib.structure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ALStructureTypes {

    private ALStructureTypes() {
    }

    private static final DeferredRegister<StructureType<?>> REGISTRY = DeferredRegister.create(Registries.STRUCTURE_TYPE, AbyssLibAtlas.NAMESPACE);

    public static final DeferredHolder<StructureType<?>, StructureType<?>> JIGSAW = REGISTRY.register("jigsaw",
            () -> (StructureType<ALJigsawStructure>) () -> ALJigsawStructure.CODEC);

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}
