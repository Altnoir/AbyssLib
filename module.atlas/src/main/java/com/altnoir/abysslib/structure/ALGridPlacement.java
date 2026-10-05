package com.altnoir.abysslib.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

// 将同一 profile 应用于结构足迹内的所有区块。
public class ALGridPlacement extends RandomSpreadStructurePlacement {

    public static final MapCodec<ALGridPlacement> CODEC = RecordCodecBuilder.<ALGridPlacement>mapCodec(
            instance -> instance.group(
                            // 跨包访问 protected 成员需要显式写出接收者类型。
                            Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO)
                                    .forGetter((ALGridPlacement p) -> p.locateOffset()),
                            StructurePlacement.FrequencyReductionMethod.CODEC
                                    .optionalFieldOf("frequency_reduction_method", StructurePlacement.FrequencyReductionMethod.DEFAULT)
                                    .forGetter((ALGridPlacement p) -> p.frequencyReductionMethod()),
                            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F)
                                    .forGetter((ALGridPlacement p) -> p.frequency()),
                            StructurePlacement.ExclusionZone.CODEC
                                    .optionalFieldOf("exclusion_zone")
                                    .forGetter((ALGridPlacement p) -> p.exclusionZone()),
                            RegistryFileCodec.create(ALGridProfile.KEY, ALGridProfile.CODEC)
                                    .fieldOf("grid_profile")
                                    .forGetter(ALGridPlacement::profile))
                    .apply(instance, ALGridPlacement::new));

    private final Holder<ALGridProfile> profile;

    public ALGridPlacement(Holder<ALGridProfile> profile) {
        this(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, Optional.empty(), profile);
    }

    public ALGridPlacement(
            Vec3i locateOffset,
            FrequencyReductionMethod frequencyReductionMethod,
            float frequency,
            Optional<ExclusionZone> exclusionZone,
            Holder<ALGridProfile> profile
    ) {
        // 父类仍要求这些参数；实际散布参数均由 profile 提供。
        super(locateOffset, frequencyReductionMethod, frequency, 0, exclusionZone, 1, 0, RandomSpreadType.LINEAR);
        this.profile = profile;
    }

    public Holder<ALGridProfile> profile() {
        return this.profile;
    }

    @Override
    protected int salt() {
        return this.profile.value().salt();
    }

    @Override
    public int spacing() {
        return this.profile.value().spacing();
    }

    @Override
    public int separation() {
        return this.profile.value().separation();
    }

    @Override
    public RandomSpreadType spreadType() {
        return this.profile.value().spreadType();
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int x, int z) {
        return this.profile.value().potentialCenterChunk(seed, x, z);
    }

    private static final java.util.concurrent.atomic.AtomicBoolean FIRST_CALL_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState structureState, int x, int z) {
        ALGridProfile profile = this.profile.value();
        boolean matched = profile.nearestCenterChunk(structureState.getLevelSeed(), x, z) != null;
        if (FIRST_CALL_LOGGED.compareAndSet(false, true)) {
            AbyssLibAtlas.LOGGER.debug(
                    "[AbyssLib/Atlas] atlas:per_chunk.isPlacementChunk 首次被调用：chunk=({}, {}) 命中={} (spacing={}, separation={}, footprint={})",
                    x, z, matched, profile.spacing(), profile.separation(), profile.footprintChunks());
        }
        return matched;
    }

    @Override
    public boolean applyAdditionalChunkRestrictions(int x, int z, long levelSeed) {
        if (this.frequency() >= 1.0F) {
            return true;
        }
        // 在中心判断频率，避免同一足迹内的区块结果不一致。
        ChunkPos center = this.profile.value().nearestCenterChunk(levelSeed, x, z);
        int cx = center != null ? center.x() : x;
        int cz = center != null ? center.z() : z;
        return this.frequencyReductionMethod().shouldGenerate(levelSeed, this.salt(), cx, cz, this.frequency());
    }

    @Override
    public StructurePlacementType<?> type() {
        return ALStructurePlacements.PER_CHUNK.get();
    }
}
