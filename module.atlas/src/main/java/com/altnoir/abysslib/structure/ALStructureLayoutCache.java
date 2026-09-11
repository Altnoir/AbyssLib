package com.altnoir.abysslib.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * <b>按 cell 缓存结构布局</b>（决策 2）。
 *
 * <h2>为什么必须有它</h2>
 * 原版一个结构只在<b>中心 chunk</b> 算一次 jigsaw 展开，结果进 NBT 后永不重算 —— 原版压根不需要缓存。
 * 但 per-chunk placement 下，足迹内<b>每个</b> chunk 都会走一遍 {@code createStructures → findGenerationPoint}，
 * 不做缓存就是原版的 (2r+1)² 倍计算量（r=16 时 1089 倍）。缓存后每个 cell 只展开一次。
 *
 * <h2>正确性前提（三条，缺一不可）</h2>
 * <ol>
 *   <li><b>不改变结果</b>：布局必须用"中心 chunk 播种的 RNG + 中心锚点"计算，这样同一 cell 在任何 chunk 上
 *       算出的布局完全一致 —— 命中与否结果相同，缓存才是透明的。</li>
 *   <li><b>可失效</b>：数据包重载后 profile/结构 JSON 可能变了 → 由 {@code AddReloadListenerEvent} 清空。</li>
 *   <li><b>线程安全</b>：worldgen 是多线程，用 {@link ConcurrentHashMap#computeIfAbsent}。
 *       {@code JigsawPlacement.addPieces} 只依赖传入的 {@code GenerationContext}（registryAccess /
 *       chunkGenerator / randomState / templateManager），<b>不碰 level</b>，因此可安全并发。</li>
 * </ol>
 *
 * <h2>内存上界</h2>
 * 每个条目是一份 piece 列表。这里加了一道粗暴但有效的上界：超过 {@link #MAX_ENTRIES} 就整体清空
 * （缓存永远只是"省重复计算"，清空不影响正确性，只是下次要重算）。
 */
public final class ALStructureLayoutCache {

    private ALStructureLayoutCache() {
    }

    /** 上界：超过就整体清空（见类注释"内存上界"）。 */
    private static final int MAX_ENTRIES = 256;

    private static final Map<Key, Layout> CACHE = new ConcurrentHashMap<>();

    /**
     * 缓存键。{@code generator} 直接参与 equals（{@code ChunkGenerator} 未重写 equals，即按实例比较）
     * —— 每个维度一个实例，于是天然把维度区分开了，不需要额外的维度 key。
     * {@code profile} 是 record，按内容比较：同内容的 profile 共用缓存没有副作用。
     */
    public record Key(ChunkGenerator generator, ALGridProfile profile, long cell) {
    }

    /**
     * 一份布局：结构中心锚点 + 完整 piece 列表 + <b>按 chunk 预索引</b>的取用表。
     *
     * <p>为什么要索引：per-chunk 方案下足迹可达 (2r+1)² 个 chunk，若每个 chunk 都去遍历全量 piece 做
     * bbox 相交测试，成本就是 <b>O(足迹面积 × piece 数)</b> —— 512 半径（4225 chunk）时不可接受。
     * 索引在布局构建时算一次，之后每个 chunk 的取用是 O(1)。
     */
    public record Layout(BlockPos anchor, List<StructurePiece> pieces, Map<Long, List<StructurePiece>> piecesByChunk) {

        /** 本 chunk 需要落地的 piece（O(1) 查表）。 */
        public List<StructurePiece> forChunk(ChunkPos chunkPos) {
            return this.piecesByChunk.getOrDefault(ChunkPos.asLong(chunkPos.x, chunkPos.z), List.of());
        }

        /**
         * 建索引：把每个 piece 登记到"其包围盒 ∪ (chunk ±1 chunk 写入区)"相交的所有 chunk 上。
         *
         * <p>与 {@code ALStructureDatagen} 里 B′ 的过滤条件等价（±1 chunk = FEATURES 的
         * {@code blockStateWriteRadius(1)}）。边界用 {@code (min-16)>>4 .. (max+16)>>4}，
         * 相比精确条件最多<b>多覆盖一个 chunk</b>（宁可多带一片，也不会漏）—— 多带的那片在
         * {@code StructureStart#placeInChunk} 里仍会被真实写入区过滤掉，只是 NBT 多几条。
         */
        public static Map<Long, List<StructurePiece>> index(List<StructurePiece> pieces) {
            Map<Long, List<StructurePiece>> map = new HashMap<>();
            for (StructurePiece piece : pieces) {
                BoundingBox box = piece.getBoundingBox();
                int minChunkX = (box.minX() - 16) >> 4;
                int maxChunkX = (box.maxX() + 16) >> 4;
                int minChunkZ = (box.minZ() - 16) >> 4;
                int maxChunkZ = (box.maxZ() + 16) >> 4;
                for (int x = minChunkX; x <= maxChunkX; x++) {
                    for (int z = minChunkZ; z <= maxChunkZ; z++) {
                        map.computeIfAbsent(ChunkPos.asLong(x, z), k -> new ArrayList<>(2)).add(piece);
                    }
                }
            }
            return map;
        }
    }

    public static Layout get(Key key, Supplier<Layout> factory) {
        if (CACHE.size() >= MAX_ENTRIES) {
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(key, k -> factory.get());
    }

    /** 数据包重载时调用（旧 profile 对象已失效，且新 profile 内容可能变化）。 */
    public static void clear() {
        CACHE.clear();
    }

    public static int size() {
        return CACHE.size();
    }
}
