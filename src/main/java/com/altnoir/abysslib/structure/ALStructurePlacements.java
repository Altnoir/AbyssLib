package com.altnoir.abysslib.structure;

import com.altnoir.abysslib.AbyssLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * AbyssLib 注册的原版世界生成扩展类型。
 *
 * <p>注意 {@code Registries.STRUCTURE_PLACEMENT} 是一个<b>内置（static）注册表</b>
 * （原版在 {@code StructurePlacementType} 里用 {@code Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, ...)}
 * 直接注册），所以这里用 {@code DeferredRegister} 挂在 mod 总线上即可，不需要数据包。
 */
public final class ALStructurePlacements {

    private ALStructurePlacements() {
    }

    private static final DeferredRegister<StructurePlacementType<?>> REGISTRY =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, AbyssLib.MOD_ID);

    /** {@code abysslib:per_chunk} —— 逐 chunk 放置，供超大结构使用；详见 {@link ALGridPlacement}。 */
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<?>> PER_CHUNK =
            REGISTRY.register("per_chunk", () -> (StructurePlacementType<ALGridPlacement>) () -> ALGridPlacement.CODEC);

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}
