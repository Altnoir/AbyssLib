package com.altnoir.abysslib.structure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ALStructurePlacements {

    private ALStructurePlacements() {
    }

    private static final DeferredRegister<StructurePlacementType<?>> REGISTRY =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, AbyssLibAtlas.NAMESPACE);

    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<?>> PER_CHUNK =
            REGISTRY.register("per_chunk", () -> (StructurePlacementType<ALGridPlacement>) () -> ALGridPlacement.CODEC);

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}
