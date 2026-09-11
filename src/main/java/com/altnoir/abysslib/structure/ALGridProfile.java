package com.altnoir.abysslib.structure;

import com.altnoir.abysslib.AbyssLib;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import javax.annotation.Nullable;

/**
 * <b>网格 profile（决策"乙"的载体）</b>：把"大结构的网格参数"收进一个数据包注册表元素里，
 * 让 per-chunk placement（{@link ALGridPlacement}）与结构类型（{@code abysslib:jigsaw}）都只
 * <b>引用同一个 id</b>，从而在结构上杜绝"两边参数不一致 → 结构碎掉"这个新失败模式。
 *
 * <p>为什么需要它：原版结构类型<b>不需要</b>知道网格参数（锚点就是"传进来的那个 chunk"），
 * 所以原版只有 placement 一处参数、物理上不可能不一致。我们的 {@code abysslib:jigsaw} 必须
 * 自己复算"结构中心"，于是同一组参数会出现在两个 JSON 里 —— 单一数据源就是解法。
 *
 * <p>数据包路径：{@code data/<namespace>/abysslib/grid_profile/<name>.json}
 *
 * <pre>{@code
 * { "spacing": 64, "separation": 32, "spread_type": "linear", "salt": 10387312, "footprint_chunks": 8 }
 * }</pre>
 *
 * @param spacing          网格单元边长（chunk），必须 &gt; separation
 * @param separation       中心在单元内的最小退让（chunk）
 * @param spreadType       与原版一致：linear / triangular
 * @param salt             与原版一致的盐，决定中心落在单元里的位置
 * @param footprintChunks  结构中心到足迹边缘的 chunk 数（= max_distance_from_center / 16，向上取整）
 */
public record ALGridProfile(
        int spacing,
        int separation,
        RandomSpreadType spreadType,
        int salt,
        int footprintChunks
) {

    /** 注册表 id：{@code abysslib:grid_profile}（数据包目录 {@code abysslib/grid_profile/}）。 */
    public static final ResourceKey<Registry<ALGridProfile>> KEY =
            ResourceKey.createRegistryKey(AbyssLib.loc("grid_profile"));

    public static final Codec<ALGridProfile> CODEC = RecordCodecBuilder.<ALGridProfile>create(
                    instance -> instance.group(
                                    Codec.intRange(0, 4096).fieldOf("spacing").forGetter(ALGridProfile::spacing),
                                    Codec.intRange(0, 4096).fieldOf("separation").forGetter(ALGridProfile::separation),
                                    RandomSpreadType.CODEC
                                            .optionalFieldOf("spread_type", RandomSpreadType.LINEAR)
                                            .forGetter(ALGridProfile::spreadType),
                                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(ALGridProfile::salt),
                                    Codec.intRange(1, 512).fieldOf("footprint_chunks").forGetter(ALGridProfile::footprintChunks))
                            .apply(instance, ALGridProfile::new))
            .validate(ALGridProfile::validate);

    /**
     * 校验放在<b>这里</b>（而不是 placement / 结构的 codec 里）的原因：profile 是唯一数据源，
     * 校验一次就同时约束了两侧；而且这里拿到的都是普通字段，不涉及"数据包加载期 Holder 未绑定"的坑。
     */
    private static DataResult<ALGridProfile> validate(ALGridProfile profile) {
        if (profile.spacing() <= profile.separation()) {
            // spacing <= separation 会让原版算法里的 spacing - separation <= 0，
            // 进而在 spreadType.evaluate → rng.nextInt(k) 上抛 IllegalArgumentException，必须提前拦掉
            return DataResult.error(() -> "abysslib:grid_profile 要求 spacing > separation（当前 "
                    + profile.spacing() + " <= " + profile.separation() + "）");
        }
        if (profile.footprintChunks() * 2 >= profile.spacing()) {
            return DataResult.error(() -> "abysslib:grid_profile 的 footprint_chunks 过大：要求 footprint_chunks * 2 < spacing，"
                    + "否则相邻结构的足迹会重叠（当前 footprint_chunks=" + profile.footprintChunks()
                    + ", spacing=" + profile.spacing() + "）");
        }
        return DataResult.success(profile);
    }

    /** 数据包注册表注册（由 {@link AbyssLib} 的构造器挂到 mod 总线）。 */
    public static void registerDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(KEY, CODEC, CODEC);
    }

    /**
     * <b>便利工厂</b>：由"结构半径（格）"自动派生 {@code footprint_chunks}（= {@code ceil(半径 / 16)}），
     * 让消费方不必手算这个最容易写错的字段，并且在<b>构造期</b>就把配对约束查出来
     * （比等到数据包加载时报错友好得多）。
     *
     * @param maxDistanceFromCenter 结构的 {@code max_distance_from_center}（格）
     * @param spacingChunks         网格单元边长（chunk）
     * @param separationChunks      中心在单元内的最小退让（chunk）
     */
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

    /** 同上，`spread_type = linear`、`separation = 1`。 */
    public static ALGridProfile forRadius(int maxDistanceFromCenter, int spacingChunks, int salt) {
        return forRadius(maxDistanceFromCenter, spacingChunks, 1, RandomSpreadType.LINEAR, salt);
    }

    /**
     * 复用原版随机散布算法构造"算法载体"。
     * <p>刻意不自己重写中心算法（floorDiv + setLargeFeatureWithSalt + spreadType.evaluate 那几行）：
     * 自己抄一份就会有和原版 divergence 的风险，而这里可以<b>直接用原版的公开构造器与公开方法</b>。
     */
    private RandomSpreadStructurePlacement math() {
        return new RandomSpreadStructurePlacement(this.spacing, this.separation, this.spreadType, this.salt);
    }

    /** 区域坐标 → 该区域的结构中心 chunk（原版 {@code getPotentialStructureChunk}）。 */
    public ChunkPos potentialCenterChunk(long seed, int regionX, int regionZ) {
        return this.math().getPotentialStructureChunk(seed, regionX, regionZ);
    }

    /**
     * 求包含 (x,z) 的足迹所属中心 chunk；不在任何足迹内则返回 null。
     * <p>扫描 3×3 个相邻 cell（靠近单元边界的 chunk 可能落在隔壁 cell 的足迹里），取距离最近者。
     * <p><b>placement 与结构类型必须共用这一份实现</b> —— 这是"乙"的核心价值所在。
     */
    @Nullable
    public ChunkPos nearestCenterChunk(long seed, int x, int z) {
        final int spacing = Math.max(1, this.spacing);
        final RandomSpreadStructurePlacement math = this.math();
        int bestDistance = Integer.MAX_VALUE;
        ChunkPos best = null;

        for (int dx = -spacing; dx <= spacing; dx += spacing) {
            for (int dz = -spacing; dz <= spacing; dz += spacing) {
                ChunkPos center = math.getPotentialStructureChunk(seed, x + dx, z + dz);
                int distance = Math.max(Math.abs(center.x - x), Math.abs(center.z - z));
                if (distance <= this.footprintChunks && distance < bestDistance) {
                    bestDistance = distance;
                    best = center;
                }
            }
        }
        return best;
    }
}
