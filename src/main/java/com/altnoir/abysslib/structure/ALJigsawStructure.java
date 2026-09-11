package com.altnoir.abysslib.structure;

import com.altnoir.abysslib.AbyssLib;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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

/**
 * <b>{@code abysslib:jigsaw}</b> —— 与 {@code abysslib:per_chunk} 放置配套的 jigsaw 结构类型，
 * 用来让<b>超过原版 128 格</b>的结构（典型场景：长道路、巨型地牢）能够完整生成。
 *
 * <h2>为什么原版 {@code minecraft:jigsaw} 配 per-chunk 会碎</h2>
 * 原版 {@code JigsawStructure#findGenerationPoint} 用 {@code context.chunkPos()} 当锚点，
 * 而且 {@code context.random()} 的种子也来自该 chunkPos。footprint 内每个 chunk 都会各自算一次，
 * 于是<b>每个 chunk 得到完全不同的布局</b> → 碎。
 *
 * <h2>本类型的三个关键改动</h2>
 * <ol>
 *   <li><b>锚点固定在中心</b>：由 {@link ALGridProfile#nearestCenterChunk} 求出所属 cell 的中心 chunk，
 *       在中心采样 {@code start_height}，以中心坐标作为 {@code addPieces} 的起点；</li>
 *   <li><b>RNG 用中心播种</b>：用"中心 chunk"重建一个 {@link Structure.GenerationContext}（原版 9 参构造器会据此
 *       生成 RNG），于是同一 cell 在任何 chunk 上算出的布局完全一致 —— 这既是正确性要求，也是
 *       {@link ALStructureLayoutCache} 能成立的前提；</li>
 *   <li><b>piece 按"本地写入区"过滤（决策 B′）</b>：只保留与本 chunk ±1 chunk 写入区相交的 piece。
 *       ±1 chunk 正好等于 FEATURES 的 {@code blockStateWriteRadius(1)}（也就是 {@code placeInChunk} 拿到的 box），
 *       所以<b>不会丢任何本 chunk 该铺的方块</b>；同时它让相邻 chunk 的 start 包围盒仍然与本 chunk 相交 →
 *       {@code createReferences} 会建立引用 → {@code Beardifier} 需要的"距本 chunk 12 格内的 piece"
 *       （{@code Beardifier#forStructuresInChunk} 的 {@code isCloseToChunk(chunkPos, 12)}）全都能被解析到 →
 *       <b>地形贴合与原版等价</b>。
 *       （若 margin 取 0，12 格内核会缺片，chunk 边界出现贴合接缝 —— 风险在 margin，不在过滤本身。）</li>
 * </ol>
 *
 * <h2>biome 判定为什么天然一致</h2>
 * 原版 {@code Structure#findValidGenerationPoint} 是 {@code findGenerationPoint(ctx).filter(stub -> isValidBiome(stub, ctx))}，
 * 而 {@code isValidBiome} 采样的是 <b>{@code stub.position()}</b> 处的 biome。本类型把 stub 的位置设为
 * <b>中心锚点</b>，所以足迹内每个 chunk 的 biome 判定完全相同（原版按各自 chunkPos 采样的做法在 per-chunk 下会造成碎片）。
 *
 * <h2>使用要求</h2>
 * <ul>
 *   <li>structure_set 必须用 {@code abysslib:per_chunk}，否则只有中心 chunk 有 start，远处 piece 依旧不落块；</li>
 *   <li>{@code grid_profile} 必须与 placement 引用<b>同一个</b> profile id（这是"乙"方案的作用：单一数据源，不可能不一致）；</li>
 *   <li>{@code footprint_chunks * 16} 必须 ≥ {@code max_distance_from_center}，否则布局会超出足迹（运行时会打一次 WARN）；</li>
 *   <li>用 {@code projection: terrain_matching} 做道路时，{@code terrain_adaptation} 用 {@code none}
 *       （{@code Beardifier} 只处理 {@code rigid} 的 piece，对 terrain_matching 设 beard 等于没设）。</li>
 * </ul>
 */
public class ALJigsawStructure extends Structure {

    public static final MapCodec<ALJigsawStructure> CODEC = RecordCodecBuilder.<ALJigsawStructure>mapCodec(
            instance -> instance.group(
                            settingsCodec(instance),
                            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                            ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                            Codec.intRange(0, 128).fieldOf("size").forGetter(s -> s.maxDepth),
                            HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                            Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                            Codec.intRange(1, 512).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                            Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(s -> s.poolAliases),
                            DimensionPadding.CODEC
                                    .optionalFieldOf("dimension_padding", JigsawStructure.DEFAULT_DIMENSION_PADDING)
                                    .forGetter(s -> s.dimensionPadding),
                            LiquidSettings.CODEC
                                    .optionalFieldOf("liquid_settings", JigsawStructure.DEFAULT_LIQUID_SETTINGS)
                                    .forGetter(s -> s.liquidSettings),
                            RegistryFileCodec.create(ALGridProfile.KEY, ALGridProfile.CODEC).fieldOf("grid_profile").forGetter(s -> s.gridProfile))
                    .apply(instance, ALJigsawStructure::new));

    /** 同一条警告只打一次，避免每个 cell 刷屏。 */
    private static final AtomicBoolean FOOTPRINT_WARNED = new AtomicBoolean(false);

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final List<PoolAliasBinding> poolAliases;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;
    private final Holder<ALGridProfile> gridProfile;

    /**
     * <b>datagen / 代码构造用的便利构造器</b>（对应原版 `JigsawStructure` 的同名重载，只多一个 profile）：
     * `start_jigsaw_name = empty`、`pool_aliases = []`、`dimension_padding / liquid_settings` 用原版默认值。
     *
     * <pre>{@code
     * // 消费方的 RegistrySetBuilder#bootstrap(Registries.STRUCTURE)：
     * Holder<ALGridProfile> profile = context.lookup(ALGridProfile.KEY).getOrThrow(MyProfiles.ROAD);
     * Holder<StructureTemplatePool> pool = context.lookup(Registries.TEMPLATE_POOL).getOrThrow(MyPools.ROAD_START);
     * context.register(MyStructures.ROAD, new ALJigsawStructure(
     *         new Structure.StructureSettings.Builder(biome.getOrThrow(MyTags.HAS_ROAD))
     *                 .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
     *                 .terrapAdapation(TerrainAdjustment.NONE)   // 道路用 terrain_matching 时应为 NONE
     *                 .build(),
     *         pool, 32, ConstantHeight.of(VerticalAnchor.absolute(0)), false,
     *         Heightmap.Types.WORLD_SURFACE_WG, 128, profile));
     * }</pre>
     *
     * <p>⚠️ 这里<b>不能</b>用 {@code profile.value()} 去推导 {@code max_distance_from_center}：
     * datagen 的 {@code BootstrapContext.lookup(...)} 返回的 Holder 在 bootstrap 期间<b>尚未绑定</b>，
     * 调用 {@code value()} 会抛异常。所以最大距离由调用方显式给出。
     */
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

    /** 同上，但 `project_start_to_heightmap` 可缺省。 */
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
            Optional<ResourceLocation> startJigsawName,
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
        // 运行时才 .value()：数据包加载期 Holder 可能尚未绑定，构造器里解析会踩坑
        final ALGridProfile profile = this.gridProfile.value();
        final ChunkPos here = context.chunkPos();
        final ChunkPos center = profile.nearestCenterChunk(context.seed(), here.x, here.z);
        if (center == null) {
            // 本 chunk 不在任何足迹内 —— 交给 placement 判定即可，这里直接不生成
            return Optional.empty();
        }

        final LevelHeightAccessor heightAccessor = context.heightAccessor();
        if (this.maxDistanceFromCenter > profile.footprintChunks() * 16 && FOOTPRINT_WARNED.compareAndSet(false, true)) {
            AbyssLib.LOGGER.warn(
                    "[AbyssLib] abysslib:jigsaw 的 max_distance_from_center={} 超出了 grid_profile 的足迹半径 {} 格"
                            + "（footprint_chunks={}）；超出部分不会被生成，请调大 footprint_chunks 或调小 max_distance_from_center。",
                    this.maxDistanceFromCenter, profile.footprintChunks() * 16, profile.footprintChunks());
        }

        final ALStructureLayoutCache.Layout layout = ALStructureLayoutCache.get(
                new ALStructureLayoutCache.Key(context.chunkGenerator(), profile, ChunkPos.asLong(center.x, center.z)),
                () -> this.buildLayout(context, profile, center, heightAccessor));

        if (layout.pieces().isEmpty()) {
            return Optional.empty();
        }

        // 决策 B′：只保留与本 chunk 写入区（±1 chunk）相交的 piece —— 已按 chunk 预索引，这里 O(1) 取用
        final List<StructurePiece> local = layout.forChunk(here);
        if (local.isEmpty()) {
            AbyssLib.LOGGER.debug("[AbyssLib] abysslib:jigsaw @ {} 在中心 {} 的足迹内，但本 chunk 无相交 piece（跳过）",
                    here, center);
            return Optional.empty();
        }

        AbyssLib.LOGGER.debug(
                "[AbyssLib] abysslib:jigsaw @ {}（中心 {}，锚点 {}）：布局 {} 片，本 chunk 取 {} 片",
                here, center, layout.anchor(), layout.pieces().size(), local.size());

        // stub 的位置用"中心锚点"：isValidBiome 按 stub.position() 采样，这样足迹内每个 chunk 的 biome 判定一致
        return Optional.of(new Structure.GenerationStub(layout.anchor(), builder -> local.forEach(builder::addPiece)));
    }

    /**
     * 按 cell 计算完整布局（每个 cell 只跑一次，见 {@link ALStructureLayoutCache}）。
     * <p>注意这里刻意用"中心 chunk"重建 context：原版 9 参构造器会据此生成 RNG，
     * 于是 {@code startHeight.sample} 与 {@code JigsawPlacement.addPieces} 内部的一切随机都与中心绑定。
     */
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

        // 复用原版公开的拼装入口，不抄任何拼装算法
        final Optional<Structure.GenerationStub> stub = JigsawPlacement.addPieces(
                centered,
                this.startPool,
                this.startJigsawName,
                this.maxDepth,
                anchor,
                this.useExpansionHack,
                this.projectStartToHeightmap,
                this.maxDistanceFromCenter,
                PoolAliasLookup.create(this.poolAliases, anchor, centered.seed()),
                this.dimensionPadding,
                this.liquidSettings);

        return stub.map(s -> {
                    List<StructurePiece> pieces = List.copyOf(s.getPiecesBuilder().build().pieces());
                    // 索引在此构建（每个 cell 一次），把 per-chunk 的取用成本从 O(全量 piece) 降到 O(1)
                    return new ALStructureLayoutCache.Layout(anchor, pieces, ALStructureLayoutCache.Layout.index(pieces));
                })
                .orElseGet(() -> new ALStructureLayoutCache.Layout(anchor, List.of(), Map.of()));
    }

    @Override
    public StructureType<?> type() {
        return ALStructureTypes.JIGSAW.get();
    }
}
