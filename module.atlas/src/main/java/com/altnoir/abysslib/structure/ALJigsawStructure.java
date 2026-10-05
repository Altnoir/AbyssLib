package com.altnoir.abysslib.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

// 在 profile 足迹内共享以中心为锚点的 jigsaw 布局。
public class ALJigsawStructure extends Structure {

    public static final MapCodec<ALJigsawStructure> CODEC = RecordCodecBuilder.<ALJigsawStructure>mapCodec(
            instance -> instance.group(
                            settingsCodec(instance),
                            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                            Identifier.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                            Codec.intRange(0, 128).fieldOf("size").forGetter(s -> s.maxDepth),
                            HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                            Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                            Codec.intRange(1, ALStructureLimits.JIGSAW_MAX_DISTANCE_FROM_CENTER).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                            Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(s -> s.poolAliases),
                            DimensionPadding.CODEC
                                    .optionalFieldOf("dimension_padding", JigsawStructure.DEFAULT_DIMENSION_PADDING)
                                    .forGetter(s -> s.dimensionPadding),
                            LiquidSettings.CODEC
                                    .optionalFieldOf("liquid_settings", JigsawStructure.DEFAULT_LIQUID_SETTINGS)
                                    .forGetter(s -> s.liquidSettings),
                            RegistryFileCodec.create(ALGridProfile.KEY, ALGridProfile.CODEC).fieldOf("grid_profile").forGetter(s -> s.gridProfile))
                    .apply(instance, ALJigsawStructure::new));

    private static final AtomicBoolean FOOTPRINT_WARNED = new AtomicBoolean(false);

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<Identifier> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final List<PoolAliasBinding> poolAliases;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;
    private final Holder<ALGridProfile> gridProfile;

    public ALJigsawStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Heightmap.Types projectStartToHeightmap,
            int maxDistanceFromCenter,
            Holder<ALGridProfile> gridProfile
    ) {
        this(settings, startPool, Optional.empty(), maxDepth, startHeight, useExpansionHack,
                Optional.of(projectStartToHeightmap), maxDistanceFromCenter, List.of(),
                JigsawStructure.DEFAULT_DIMENSION_PADDING, JigsawStructure.DEFAULT_LIQUID_SETTINGS, gridProfile);
    }

    public ALJigsawStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            Holder<ALGridProfile> gridProfile
    ) {
        this(settings, startPool, Optional.empty(), maxDepth, startHeight, useExpansionHack,
                projectStartToHeightmap, maxDistanceFromCenter, List.of(),
                JigsawStructure.DEFAULT_DIMENSION_PADDING, JigsawStructure.DEFAULT_LIQUID_SETTINGS, gridProfile);
    }

    public ALJigsawStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            Optional<Identifier> startJigsawName,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            List<PoolAliasBinding> poolAliases,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings,
            Holder<ALGridProfile> gridProfile
    ) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.poolAliases = poolAliases;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
        this.gridProfile = gridProfile;
    }

    public Holder<ALGridProfile> gridProfile() {
        return this.gridProfile;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        // Holder 在数据包加载完成后才能解析。
        final ALGridProfile profile = this.gridProfile.value();
        final ChunkPos here = context.chunkPos();
        final ChunkPos center = profile.nearestCenterChunk(context.seed(), here.x(), here.z());
        if (center == null) {
            return Optional.empty();
        }

        final LevelHeightAccessor heightAccessor = context.heightAccessor();
        if (this.maxDistanceFromCenter > profile.footprintChunks() * 16 && FOOTPRINT_WARNED.compareAndSet(false, true)) {
            AbyssLibAtlas.LOGGER.warn(
                    "[AbyssLib/Atlas] atlas:jigsaw 的 max_distance_from_center={} 超出了 grid_profile 的足迹半径 {} 格"
                            + "（footprint_chunks={}）；超出部分不会被生成，请调大 footprint_chunks 或调小 max_distance_from_center。",
                    this.maxDistanceFromCenter, profile.footprintChunks() * 16, profile.footprintChunks());
        }

        final ALStructureLayoutCache.Layout layout = ALStructureLayoutCache.get(
                new ALStructureLayoutCache.Key(context.chunkGenerator(), profile, ChunkPos.pack(center.x(), center.z())),
                () -> this.buildLayout(context, profile, center, heightAccessor));

        if (layout.pieces().isEmpty()) {
            return Optional.empty();
        }

        final List<StructurePiece> local = layout.forChunk(here);
        if (local.isEmpty()) {
            AbyssLibAtlas.LOGGER.debug("[AbyssLib/Atlas] atlas:jigsaw @ {} 在中心 {} 的足迹内，但本 chunk 无相交 piece（跳过）",
                    here, center);
            return Optional.empty();
        }

        AbyssLibAtlas.LOGGER.debug(
                "[AbyssLib/Atlas] atlas:jigsaw @ {}（中心 {}，锚点 {}）：布局 {} 片，本 chunk 取 {} 片",
                here, center, layout.anchor(), layout.pieces().size(), local.size());

        // 统一锚点，确保足迹内的生物群系检查结果一致。
        return Optional.of(new Structure.GenerationStub(layout.anchor(), builder -> local.forEach(builder::addPiece)));
    }

    private ALStructureLayoutCache.Layout buildLayout(
            Structure.GenerationContext incoming,
            ALGridProfile profile,
            ChunkPos center,
            LevelHeightAccessor heightAccessor
    ) {
        final Structure.GenerationContext centered = new Structure.GenerationContext(
                incoming.registryAccess(),
                incoming.chunkGenerator(),
                incoming.biomeSource(),
                incoming.randomState(),
                incoming.structureTemplateManager(),
                incoming.seed(),
                center,
                heightAccessor,
                incoming.validBiome());

        final int y = this.startHeight.sample(
                centered.random(), new WorldGenerationContext(centered.chunkGenerator(), heightAccessor));
        final BlockPos anchor = new BlockPos(center.getMinBlockX(), y, center.getMinBlockZ());

        final Optional<Structure.GenerationStub> stub = JigsawPlacement.addPieces(
                centered,
                this.startPool,
                this.startJigsawName,
                this.maxDepth,
                anchor,
                this.useExpansionHack,
                this.projectStartToHeightmap,
                new JigsawStructure.MaxDistance(this.maxDistanceFromCenter),
                PoolAliasLookup.create(this.poolAliases, anchor, centered.seed()),
                this.dimensionPadding,
                this.liquidSettings);

        return stub.map(s -> {
                    List<StructurePiece> pieces = List.copyOf(s.getPiecesBuilder().build().pieces());
                    // 每份布局只构建一次区块索引。
                    return new ALStructureLayoutCache.Layout(anchor, pieces, ALStructureLayoutCache.Layout.index(pieces));
                })
                .orElseGet(() -> new ALStructureLayoutCache.Layout(anchor, List.of(), Map.of()));
    }

    @Override
    public StructureType<?> type() {
        return ALStructureTypes.JIGSAW.get();
    }
}
