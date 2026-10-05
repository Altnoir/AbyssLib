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
        int yMin = level.getMinY();
        int yMax = level.getMaxY();
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
                        // 更小半径已检查过内部位置。
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
