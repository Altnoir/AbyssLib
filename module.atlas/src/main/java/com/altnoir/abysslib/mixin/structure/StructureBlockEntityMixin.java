package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 结构方块单轴上限 48 → {@link ALStructureLimits#STRUCTURE_BLOCK_MAX_SIZE}（128），
 * 并顺带优化 Detect（探测尺寸）的角块搜索。
 *
 * <p>原版位置（1.21.1）：
 * <ul>
 *   <li>{@code StructureBlockEntity#loadAdditional}：6 处内联字面量 48 / -48（posX/Y/Z 与 sizeX/Y/Z）</li>
 *   <li>{@code StructureBlockEntity#detectSize}：5 处内联字面量 80（扫描盒 ±80）</li>
 * </ul>
 *
 * <p><b>为什么 {@code loadAdditional} 必须改</b>：客户端是通过
 * {@code ClientboundBlockEntityDataPacket} → {@code loadAdditional} 同步结构方块数据的，
 * 不改这里，客户端会把收到的尺寸重新夹回 48，表现为"服务端存了 128，一打开界面又变回 48"。
 */
@Mixin(value = StructureBlockEntity.class)
public abstract class StructureBlockEntityMixin {

    @ModifyConstant(method = "loadAdditional", constant = @Constant(intValue = 48), require = 0)
    private int abysslib$widenSizeUpper(int original) {
        ALStructureLimits.structureBlockSizeApplied = true;
        return ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE;
    }

    @ModifyConstant(method = "loadAdditional", constant = @Constant(intValue = -48), require = 0)
    private int abysslib$widenOffsetLower(int original) {
        ALStructureLimits.structureBlockSizeApplied = true;
        return -ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE;
    }

    @ModifyConstant(method = "detectSize", constant = @Constant(intValue = 80), require = 0)
    private int abysslib$widenDetectRange(int original) {
        ALStructureLimits.detectRangeApplied = true;
        return ALStructureLimits.STRUCTURE_BLOCK_DETECT_RANGE;
    }

    /**
     * 把原版"扫满整个长方体"的角块搜索换成"由中心向外的立方壳层搜索 + 命中即停 + 检查预算熔断"。
     *
     * <p>原版：{@code BlockPos.betweenClosedStream(min, max)} 穷举 (2r+1)² × 世界高度；
     * r=80 时约 1000 万格，r=128 时约 2500 万格 —— 按一下 Detect 就是几秒到几十秒的服务端卡顿。
     *
     * <p>Integrated API 的做法是把这里 {@code @Overwrite} 成 {@code BlockPos.findClosestMatch}
     * （由近到远、找到 2 个角块即停），这是它"512 也不卡"的核心：<b>实际耗时取决于最近角块的距离，
     * 而不是配置的半径</b>。但它没有最坏情况保护 —— 当周围没有配对角块时会扫满整个半径。
     * 本实现采用同样的"扩张 + 提前退出"思路，并额外加了 {@link ALStructureLimits#CORNER_SEARCH_CHECK_BUDGET}
     * 熔断，保证最坏情况也有上界。
     *
     * <p>语义差异（有意为之，已确认）：原版会把扫描盒内<b>所有</b>同名校角块都纳入包围盒，
     * 本实现最多收集 2 个（正常的结构方块用法就是 1 个 CORNER + 自身 1 个 SAVE）。
     */
    @Inject(method = "getRelatedCorners", at = @At("HEAD"), cancellable = true, require = 0)
    private void abysslib$fastCornerSearch(BlockPos minPos, BlockPos maxPos, CallbackInfoReturnable<Stream<BlockPos>> cir) {
        StructureBlockEntity self = (StructureBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            cir.setReturnValue(Stream.empty());
            return;
        }

        BlockPos center = self.getBlockPos();
        String selfName = self.getStructureName();
        int radius = ALStructureLimits.STRUCTURE_BLOCK_DETECT_RANGE;
        int yMin = level.getMinBuildHeight();
        int yMax = level.getMaxBuildHeight() - 1;
        int budget = ALStructureLimits.CORNER_SEARCH_CHECK_BUDGET;

        List<BlockPos> found = new ArrayList<>(2);
        int checks = 0;

        search:
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                int ax = Math.abs(dx);
                for (int dy = -r; dy <= r; dy++) {
                    int ay = Math.abs(dy);
                    for (int dz = -r; dz <= r; dz++) {
                        // 只走第 r 层"壳"，内部已在更小的 r 里查过
                        if (Math.max(ax, Math.max(ay, Math.abs(dz))) != r) {
                            continue;
                        }
                        int y = center.getY() + dy;
                        if (y < yMin || y > yMax) {
                            continue;
                        }
                        if (++checks > budget) {
                            break search;
                        }

                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.getBlockState(pos).is(Blocks.STRUCTURE_BLOCK)) {
                            continue;
                        }
                        if (level.getBlockEntity(pos) instanceof StructureBlockEntity other
                                && other.getMode() == StructureMode.CORNER
                                && Objects.equals(selfName, other.getStructureName())) {
                            found.add(other.getBlockPos().immutable());
                            if (found.size() >= 2) {
                                break search;
                            }
                        }
                    }
                }
            }
        }

        ALStructureLimits.detectRangeApplied = true;
        cir.setReturnValue(found.stream());
    }
}
