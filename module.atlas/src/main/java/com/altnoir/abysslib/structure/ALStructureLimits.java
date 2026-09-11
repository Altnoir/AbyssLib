package com.altnoir.abysslib.structure;

/**
 * AbyssLib 对原版结构系统限制的放宽值（集中定义，便于统一调整与自检日志输出）。
 *
 * <p>背景：这些上限在原版里都是 <b>编译期内联常量</b>，例如
 * {@code StructureBlockEntity.MAX_SIZE_PER_AXIS = 48}、{@code JigsawStructure.MAX_TOTAL_STRUCTURE_RANGE = 128}
 * 都是 {@code public static final int}，javac 会把字面量内联进所有调用点的字节码，
 * 因此运行时"修改常量本身"是不可能的，只能对每个使用点做 {@code @ModifyConstant} / {@code @ModifyArg}。
 * 本类只提供"目标值"，实际替换在 {@code com.altnoir.abysslib.mixin.structure} 下完成。
 *
 * <p>取值依据（2026-09 定稿，同日把 jigsaw 距离上限由 512 收紧到 256）：
 * <ul>
 *   <li>jigsaw {@code max_distance_from_center} 128 → 256（per-chunk 已解决截断，无需再抬高到 512）</li>
 *   <li>jigsaw {@code size}（层深）20 → 128（对齐 Integrated API 的做法）</li>
 *   <li>结构方块单轴尺寸/偏移 48 → 128</li>
 *   <li>结构方块名长度（客户端输入框）128 → 256</li>
 *   <li>pool 元素 weight 保持原版 150，<b>不改</b></li>
 * </ul>
 */
public final class ALStructureLimits {

    private ALStructureLimits() {
    }

    /** 原版结构方块单轴上限（{@code StructureBlockEntity.MAX_SIZE_PER_AXIS} / {@code MAX_OFFSET_PER_AXIS}），仅作判断基准用。 */
    public static final int VANILLA_STRUCTURE_BLOCK_LIMIT = 48;

    /**
     * 结构方块单轴尺寸与偏移上限（原版 48）。
     * <p>注意：128 已经超出原版网络包的 {@code byte} 编码范围（{@code ServerboundSetStructureBlockPacket}
     * 用 {@code writeByte}/{@code readByte} 传 size/offset，正数上限仅 127），因此
     * <b>必须</b>配合 {@code ServerboundSetStructureBlockPacketMixin} 的"追加载荷"方案，
     * 否则改了 UI 和 NBT 也会在收包时被夹回 48。
     */
    public static final int STRUCTURE_BLOCK_MAX_SIZE = 128;

    /**
     * SAVE 模式 "Detect"（探测结构尺寸）按钮的水平扫描半径（原版 80）。
     * <p>原版实现是 {@code betweenClosedStream} 全量穷举 {@code (2r+1)² × 世界高度}
     * （r=128 时约 2500 万格），所以本库同时把角块搜索换成了"由中心向外、命中即停"的实现，
     * 见 {@code StructureBlockEntityMixin#abysslib$fastCornerSearch}。
     */
    public static final int STRUCTURE_BLOCK_DETECT_RANGE = 128;

    /** 结构方块名最大长度（客户端输入框，原版 128）。服务端本身无长度限制。 */
    public static final int STRUCTURE_BLOCK_NAME_LENGTH = 256;

    /**
     * jigsaw {@code max_distance_from_center} 上限（原版 128；本库放宽到 256）。
     *
     * <p>⚠️ 与 {@link #STRUCTURE_BLOCK_MAX_SIZE} 不同，这个参数<b>不是惰性的</b>：它直接决定
     * {@code JigsawPlacement} 的展开盒大小，以及 per-chunk 方案下的足迹面积 ——
     * {@code 256 / 16 = 16} chunk ⇒ {@code (2·16+1)² = 1089} 个 chunk，每个都会走一次
     * {@code findGenerationPoint}（缓存命中 + 按 chunk 索引取片）。
     *
     * <p><b>为什么用 256 而不是 512</b>：原版之所以只能到 128，是因为邻居结构靠 references 传播，
     * 而那个半径是硬编码的 8 chunk（=128 格）—— 超过就会被静默丢弃。本库的 per-chunk 放置
     * （{@code atlas:per_chunk} + {@code atlas:jigsaw}）已经从机制上解决了截断问题，
     * <b>"抬高上限"能拿到的收益已经由 per-chunk 拿到</b>；继续把上限抬到 512 只会让最坏足迹
     * 从 16 chunk 涨到 32 chunk（1089 → 4225 个 chunk，<b>4 倍</b>开销）。
     * 256 已覆盖长道路 / 大城堡 / 跨群系连续结构等绝大多数需求，故把上限钉在 256。
     *
     * <p>配套约束：{@code footprint_chunks * 2 < spacing} ⇒ {@code spacing > 32} chunk（≈520 格），
     * 即巨型结构必须稀疏（每约 0.27 km² 一个 cell）—— 这顺带限制了世界里同时存在的巨型结构数量。
     */
    public static final int JIGSAW_MAX_DISTANCE_FROM_CENTER = 256;

    /** jigsaw {@code size}（jigsaw 层深）上限（原版 20，Integrated API 用 128）。 */
    public static final int JIGSAW_MAX_DEPTH = 128;

    /**
     * 角块搜索的检查预算（熔断阈值，单位=方块查询次数）。
     * <p>用途：当结构方块周围<b>不存在</b>配对的 CORNER 角块时，任何"向外扩张"的搜索都会退化成
     * 扫满整个半径。Integrated API 只是把暴力搜索换成 {@code BlockPos.findClosestMatch}（靠命中提前退出），
     * 没有最坏情况保护；本库额外加了这道熔断，保证按 Detect 永远不会卡住服务端。
     * <p>200 万次 {@code getBlockState} 量级约 0.1~0.2 秒。
     */
    public static final int CORNER_SEARCH_CHECK_BUDGET = 2_000_000;

    // ---- 自检标志：由 Mixin 的注入处理器在真正执行时置位，供 ALStructureEnhancementCheck 汇报 ----

    /** {@code StructureBlockEntity#loadAdditional} 的 48/-48 是否已被替换。 */
    public static volatile boolean structureBlockSizeApplied = false;

    /** {@code StructureBlockEntity#detectSize} 的 80 + 角块搜索优化是否已生效。 */
    public static volatile boolean detectRangeApplied = false;

    /** {@code JigsawStructure} 的 codec 范围（距离/层深）是否已被放宽。 */
    public static volatile boolean jigsawRangeApplied = false;

    /** {@code JigsawStructure#verifyRange} 的 128 上限是否已被替换。 */
    public static volatile boolean jigsawVerifyApplied = false;

    /** {@code JigsawPlacement#generateJigsaw}（/place jigsaw 命令路径）的 128 是否已被替换。 */
    public static volatile boolean placeJigsawApplied = false;
}
