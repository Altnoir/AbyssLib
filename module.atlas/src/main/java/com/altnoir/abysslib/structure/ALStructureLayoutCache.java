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

// 按生成器、profile 和网格单元缓存布局。
public final class ALStructureLayoutCache {

    private ALStructureLayoutCache() {
    }

    private static final int MAX_ENTRIES = 256;

    private static final Map<Key, Layout> CACHE = new ConcurrentHashMap<>();

    public record Key(ChunkGenerator generator, ALGridProfile profile, long cell) {
    }

    public record Layout(BlockPos anchor, List<StructurePiece> pieces, Map<Long, List<StructurePiece>> piecesByChunk) {

        public List<StructurePiece> forChunk(ChunkPos chunkPos) {
            return this.piecesByChunk.getOrDefault(ChunkPos.pack(chunkPos.x(), chunkPos.z()), List.of());
        }

        public static Map<Long, List<StructurePiece>> index(List<StructurePiece> pieces) {
            Map<Long, List<StructurePiece>> map = new HashMap<>();
            for (StructurePiece piece : pieces) {
                // 为区块写入范围保留一个区块的边界。
                BoundingBox box = piece.getBoundingBox();
                int minChunkX = (box.minX() - 16) >> 4;
                int maxChunkX = (box.maxX() + 16) >> 4;
                int minChunkZ = (box.minZ() - 16) >> 4;
                int maxChunkZ = (box.maxZ() + 16) >> 4;
                for (int x = minChunkX; x <= maxChunkX; x++) {
                    for (int z = minChunkZ; z <= maxChunkZ; z++) {
                        map.computeIfAbsent(ChunkPos.pack(x, z), k -> new ArrayList<>(2)).add(piece);
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

    public static void clear() {
        CACHE.clear();
    }

    public static int size() {
        return CACHE.size();
    }
}
