package com.altnoir.abysslib.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.lang.reflect.Method;

// 在启动和世界加载后检查可选注入点是否生效。
public final class ALStructureEnhancementCheck {

    private ALStructureEnhancementCheck() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> report("mod 加载完成", false));
    }

    public static void onServerStarted(ServerStartedEvent event) {
        report("世界数据包加载完成", true);
    }

    private static void report(String phase, boolean runtimePhase) {
        // 先初始化 codec，再检查对应的 mixin 标志。
        try {
            Class.forName("net.minecraft.world.level.levelgen.structure.structures.JigsawStructure",
                    true, ALStructureEnhancementCheck.class.getClassLoader());
        } catch (Throwable t) {
            AbyssLibAtlas.LOGGER.warn("[AbyssLib/Atlas] 无法强制初始化 JigsawStructure，jigsaw 放宽自检可能不准：{}", t.toString());
        }

        Boolean nbt = verifyStructureBlockNbt();
        boolean codec = ALStructureLimits.jigsawRangeApplied;
        boolean verify = ALStructureLimits.jigsawVerifyApplied;
        boolean placeCmd = ALStructureLimits.placeJigsawApplied;
        boolean detect = ALStructureLimits.detectRangeApplied;

        AbyssLibAtlas.LOGGER.info(
                "[AbyssLib/Atlas] 原版结构限制放宽（{}）-> jigsaw: distance={}, depth={} [codec={}, verifyRange={}, placeCommand={}]"
                        + " | 结构方块: max={}, name={}, detectRange={} [nbt={}, detect={}]",
                phase,
                ALStructureLimits.JIGSAW_MAX_DISTANCE_FROM_CENTER,
                ALStructureLimits.JIGSAW_MAX_DEPTH,
                state(codec, runtimePhase),
                state(verify, runtimePhase),
                placeCmd ? "OK" : "待命令调用",
                ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE,
                ALStructureLimits.STRUCTURE_BLOCK_NAME_LENGTH,
                ALStructureLimits.STRUCTURE_BLOCK_DETECT_RANGE,
                nbt == null ? "无法验证" : state(nbt, runtimePhase),
                detect ? "OK" : "待首次使用");

        boolean hardFailure = !codec || Boolean.FALSE.equals(nbt) || (runtimePhase && !verify);
        if (hardFailure) {
            AbyssLibAtlas.LOGGER.warn(
                    "[AbyssLib/Atlas] 有注入点未生效（通常是原版类结构变化或被其它模组覆盖）。"
                            + "排查：确认 abysslib.mixins.json 已注册、搜索启动日志里的 mixin 报错、"
                            + "并核对目标常量是否仍在原版对应方法中。");
        }
    }

    private static String state(boolean flag, boolean runtimePhase) {
        if (flag) {
            return "OK";
        }
        return runtimePhase ? "未生效" : "待运行时";
    }

    private static Boolean verifyStructureBlockNbt() {
        try {
            StructureBlockEntity blockEntity = new StructureBlockEntity(
                    BlockPos.ZERO, Blocks.STRUCTURE_BLOCK.defaultBlockState());
            CompoundTag tag = new CompoundTag();
            tag.putInt("posX", -ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE);
            tag.putInt("sizeX", ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE);

            ValueInput input = TagValueInput.create(new ProblemReporter.Collector(), RegistryAccess.EMPTY, tag);
            Method load = StructureBlockEntity.class.getDeclaredMethod("loadAdditional", ValueInput.class);
            load.setAccessible(true);
            load.invoke(blockEntity, input);

            return blockEntity.getStructureSize().getX() == ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE
                    && blockEntity.getStructurePos().getX() == -ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE;
        } catch (Throwable t) {
            AbyssLibAtlas.LOGGER.warn("[AbyssLib/Atlas] 结构方块 NBT 上限自检失败（不影响游戏）：{}", t.toString());
            return null;
        }
    }
}
