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

/**
 * <b>{@code atlas:per_chunk}</b> —— 逐 chunk 放置：让大型结构"足迹范围内的每个 chunk 都各自持有一份结构起点"。
 *
 * <h2>它解决什么问题</h2>
 * 原版结构起点只属于<b>一个</b> chunk（{@code RandomSpreadStructurePlacement#isPlacementChunk} 是
 * {@code potentialChunk == thisChunk}），chunk 之间靠 {@code ChunkGenerator#createReferences} 传播"附近有结构"，
 * 而那里的半径是<b>硬编码的 8 chunk</b>：
 * <pre>
 * int i = 8;                                   // ← 硬编码
 * for (int j1 = j - 8; j1 &lt;= j + 8; j1++)
 *     for (int k1 = k - 8; k1 &lt;= k + 8; k1++)
 *         for (StructureStart s : level.getChunk(j1, k1).getAllStarts().values())
 *             if (s.getBoundingBox().intersects(本 chunk 的 16x16)) addReferenceForStructure(...);
 * </pre>
 * <b>8 chunk = 128 格</b>，这正是 {@code JigsawStructure.MAX_TOTAL_STRUCTURE_RANGE = 128} 的来历。
 * 超过它的 piece，远处 chunk 的 references 里没有这个结构 → <b>不落块</b>（表现为"长道路/巨型结构被截断"）。
 *
 * <p>本类换思路：不指望 neighbors 传播，让足迹内每个 chunk <b>自己就有一个 start</b>。
 * 铺方块时 {@code StructureStart#placeInChunk} 本来就有 {@code piece.getBoundingBox().intersects(box)} 过滤，
 * 所以每个 chunk 只会铺自己那一片 —— 不需要任何 references 跨越。
 *
 * <h2>参数来自 grid_profile（决策"乙"）</h2>
 * spacing / separation / spread_type / salt / footprint_chunks 全部由 {@link ALGridProfile} 提供，
 * 与结构类型 {@code atlas:jigsaw} 引用<b>同一个</b> id → 单一数据源，不可能不一致。
 * 本类把读取这些值的入口（{@link #spacing()} 等）全部改写到 profile，因此<b>运行时挂载新数据包即可生效</b>。
 *
 * <h2>为什么继承 {@code RandomSpreadStructurePlacement}</h2>
 * <ol>
 *   <li>复用原版网格中心算法（{@link ALGridProfile#potentialCenterChunk} 内部就是原版
 *       {@code getPotentialStructureChunk}，结构类型用同一份实现 → 两边必然同一个中心）；</li>
 *   <li>{@code ChunkGenerator#findNearestMapStructure} <b>只显式处理</b> {@code ConcentricRingsStructurePlacement}
 *       与 {@code RandomSpreadStructurePlacement} 两类，自定义 placement 会被直接忽略
 *       → 继承之后 {@code /locate structure} 自动可用。</li>
 * </ol>
 */
public class ALGridPlacement extends RandomSpreadStructurePlacement {

    public static final MapCodec<ALGridPlacement> CODEC = RecordCodecBuilder.<ALGridPlacement>mapCodec(
            instance -> instance.group(
                            // 这几个基类访问器是 protected，跨包不能用方法引用（会报"protected 访问控制"），
                            // 必须写成显式参数类型的 lambda（原版 placementCodec 能写方法引用是因为它在基类内部）
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

    /**
     * <b>datagen / 代码构造用的便利构造器</b>：只给 profile，其余用原版默认值
     * （`locate_offset = 0`、`frequency = 1.0`、无 exclusion zone）。
     *
     * <pre>{@code
     * // 在消费方的 RegistrySetBuilder#bootstrap(Registries.STRUCTURE_SET) 里：
     * Holder<ALGridProfile> profile = context.lookup(ALGridProfile.KEY).getOrThrow(MY_PROFILE_KEY);
     * context.register(MY_SET, new StructureSet(structureHolder, new ALGridPlacement(profile)));
     * }</pre>
     */
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
        // 注意：salt 已移到 grid_profile 里（单一数据源），所以这里不再有 salt 参数 ——
        // codec 只提供 5 个值，构造器必须正好 5 个参数，否则 P5.apply 的构造器引用无法推断。
        // 传给 super 的这些值永远不会被读到：spacing()/separation()/spreadType()/salt() 以及
        // getPotentialStructureChunk 都已被本类改写到 profile（见下）。给 1/0 是为了任何意外路径也不会除零。
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

    /** 让 {@code /locate structure}（以及任何走原版算法的路径）也使用 profile 的中心算法。 */
    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int x, int z) {
        return this.profile.value().potentialCenterChunk(seed, x, z);
    }

    /**
     * 只在第一次被调用时打一条 DEBUG（排查用）：用来区分"placement 从未被咨询"（说明结构集没进
     * {@code possibleStructureSets()}）与"咨询了但从未命中"。不会刷屏。
     */
    private static final java.util.concurrent.atomic.AtomicBoolean FIRST_CALL_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /** 只要求"落在某个结构中心的足迹内"，而不是"正好是中心那个 chunk"。 */
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

    /**
     * 把频率削减的判定<b>吸附到结构中心</b>。
     * <p>原版 {@code StructurePlacement#isStructureChunk} 会把 (chunkX, chunkZ) 传进来，而 {@code frequency < 1}
     * 时 RNG 用的就是这两个坐标 —— 在 per-chunk 方案里那会导致<b>同一足迹里有的 chunk 命中、有的不命中</b>
     * （结构被打出一堆洞）。吸附到中心后整个足迹共享同一次判定。
     */
    @Override
    public boolean applyAdditionalChunkRestrictions(int x, int z, long levelSeed) {
        if (this.frequency() >= 1.0F) {
            return true;
        }
        ChunkPos center = this.profile.value().nearestCenterChunk(levelSeed, x, z);
        int cx = center != null ? center.x : x;
        int cz = center != null ? center.z : z;
        return this.frequencyReductionMethod().shouldGenerate(levelSeed, this.salt(), cx, cz, this.frequency());
    }

    @Override
    public StructurePlacementType<?> type() {
        return ALStructurePlacements.PER_CHUNK.get();
    }
}
