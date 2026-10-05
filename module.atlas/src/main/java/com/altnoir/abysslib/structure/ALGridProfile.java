package com.altnoir.abysslib.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import javax.annotation.Nullable;

// 结构放置与结构类型共用同一份 profile。
public record ALGridProfile(
        int spacing,
        int separation,
        RandomSpreadType spreadType,
        int salt,
        int footprintChunks
) {

    public static final ResourceKey<Registry<ALGridProfile>> KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(AbyssLibAtlas.NAMESPACE, "grid_profile"));

    public static final Codec<ALGridProfile> CODEC = RecordCodecBuilder.<ALGridProfile>create(
                    instance -> instance.group(
                                    Codec.intRange(0, 4096).fieldOf("spacing").forGetter(ALGridProfile::spacing),
                                    Codec.intRange(0, 4096).fieldOf("separation").forGetter(ALGridProfile::separation),
                                    RandomSpreadType.CODEC
                                            .optionalFieldOf("spread_type", RandomSpreadType.LINEAR)
                                            .forGetter(ALGridProfile::spreadType),
                                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(ALGridProfile::salt),
                                    // 具体的足迹限制在 validate() 中校验。
                                    Codec.intRange(1, 512).fieldOf("footprint_chunks").forGetter(ALGridProfile::footprintChunks))
                            .apply(instance, ALGridProfile::new))
            .validate(ALGridProfile::validate);

    private static DataResult<ALGridProfile> validate(ALGridProfile profile) {
        if (profile.spacing() <= profile.separation()) {
            // 避免原版散布算法收到非正范围。
            return DataResult.error(() -> "atlas:grid_profile 要求 spacing > separation（当前 "
                    + profile.spacing() + " <= " + profile.separation() + "）");
        }
        if (profile.footprintChunks() * 2 >= profile.spacing()) {
            return DataResult.error(() -> "atlas:grid_profile 的 footprint_chunks 过大：要求 footprint_chunks * 2 < spacing，"
                    + "否则相邻结构的足迹会重叠（当前 footprint_chunks=" + profile.footprintChunks()
                    + ", spacing=" + profile.spacing() + "）");
        }
        return DataResult.success(profile);
    }

    public static void registerDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(KEY, CODEC, CODEC);
    }

    public static ALGridProfile forRadius(
            int maxDistanceFromCenter,
            int spacingChunks,
            int separationChunks,
            RandomSpreadType spreadType,
            int salt
    ) {
        final int footprint = Math.max(1, (maxDistanceFromCenter + 15) / 16);
        if (spacingChunks <= separationChunks) {
            throw new IllegalArgumentException("ALGridProfile.forRadius: spacing 必须大于 separation（当前 "
                    + spacingChunks + " <= " + separationChunks + "）");
        }
        if (footprint * 2 >= spacingChunks) {
            throw new IllegalArgumentException("ALGridProfile.forRadius: 半径 " + maxDistanceFromCenter
                    + " 格 → footprint_chunks=" + footprint + "，要求 spacing > " + (footprint * 2)
                    + "（否则相邻结构的足迹会重叠）；当前 spacing=" + spacingChunks);
        }
        return new ALGridProfile(spacingChunks, separationChunks, spreadType, salt, footprint);
    }

    public static ALGridProfile forRadius(int maxDistanceFromCenter, int spacingChunks, int salt) {
        return forRadius(maxDistanceFromCenter, spacingChunks, 1, RandomSpreadType.LINEAR, salt);
    }

    private RandomSpreadStructurePlacement math() {
        return new RandomSpreadStructurePlacement(this.spacing, this.separation, this.spreadType, this.salt);
    }

    public ChunkPos potentialCenterChunk(long seed, int regionX, int regionZ) {
        return this.math().getPotentialStructureChunk(seed, regionX, regionZ);
    }

    @Nullable
    public ChunkPos nearestCenterChunk(long seed, int x, int z) {
        final int spacing = Math.max(1, this.spacing);
        final RandomSpreadStructurePlacement math = this.math();
        int bestDistance = Integer.MAX_VALUE;
        ChunkPos best = null;

        for (int dx = -spacing; dx <= spacing; dx += spacing) {
            for (int dz = -spacing; dz <= spacing; dz += spacing) {
                ChunkPos center = math.getPotentialStructureChunk(seed, x + dx, z + dz);
                int distance = Math.max(Math.abs(center.x() - x), Math.abs(center.z() - z));
                if (distance <= this.footprintChunks && distance < bestDistance) {
                    bestDistance = distance;
                    best = center;
                }
            }
        }
        return best;
    }
}
