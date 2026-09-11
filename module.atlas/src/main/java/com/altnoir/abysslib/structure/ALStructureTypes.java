package com.altnoir.abysslib.structure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * AbyssLib 注册的结构类型。
 *
 * <p>{@code atlas:jigsaw} = 与 per-chunk placement（{@link ALGridPlacement}）配套的结构类型：
 * 把生成锚点与 biome 判定固定在<b>结构中心</b>，从而让足迹内每个 chunk 各自生成"自己那一片"。
 * 详见 {@link ALJigsawStructure}。
 */
public final class ALStructureTypes {

    private ALStructureTypes() {
    }

    private static final DeferredRegister<StructureType<?>> REGISTRY =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, AbyssLibAtlas.NAMESPACE);

    public static final DeferredHolder<StructureType<?>, StructureType<?>> JIGSAW =
            REGISTRY.register("jigsaw", () -> (StructureType<ALJigsawStructure>) () -> ALJigsawStructure.CODEC);

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}
