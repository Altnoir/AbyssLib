package com.altnoir.abysslib.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.lang.reflect.Method;

/**
 * 启动自检：把"结构限制放宽是否真的生效"用日志说清楚，而不是让你进游戏里试。
 *
 * <p>起因：本包大量使用 {@code require = 0} 的注入（为了跨小版本不硬崩），代价是
 * <b>注入点失配时会静默失效</b>。Integrated API 全用 {@code require = 0} 且没有任何自检，
 * 结果是"看着装了、其实没生效"。这里补上验证。
 *
 * <p><b>两个汇报时机（很重要，别只留一个）</b>：
 * <ol>
 *   <li>{@link FMLCommonSetupEvent}（mod 加载完成）：此时能验证的是
 *       "类的静态初始化就已执行"的那批 —— jigsaw codec 的 {@code intRange} 注入（lambda 在
 *       {@code <clinit>} 里跑）与结构方块 NBT 回读。</li>
 *   <li>{@link ServerStartedEvent}（世界数据包加载完成）：{@code @ModifyConstant} 的处理器只在
 *       <b>目标方法被调用时</b>才执行，而 {@code JigsawStructure#verifyRange} 要等世界生成注册表
 *       解析、{@code JigsawPlacement#generateJigsaw} 要等 {@code /place jigsaw} 命令。
 *       第一版自检在 CommonSetup 就把它们报成"未生效"，是<b>时机错误</b>（实测踩过：
 *       日志里 mixin 应用时间是 00:51:08，而自检在 00:51:04 就打印了）。</li>
 * </ol>
 *
 * <p>注册方式：由 {@link AbyssLib} 的 {@code @Mod} 构造器显式注册到 mod 总线与游戏总线，
 * <b>不用</b> {@code @EventBusSubscriber}（NeoForge 21.1 里它的 {@code bus} 参数已弃用）。
 * 全部逻辑包在 try/catch 里，任何异常只降级为 WARN 日志，绝不影响游戏启动。
 */
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
        // 强制初始化 JigsawStructure：它的 CODEC 在 <clinit> 里构建，从而触发 codec 注入处理器
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
                // /place jigsaw 只有命令被调用过才会置位，任何阶段都不算失败
                placeCmd ? "OK" : "待命令调用",
                ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE,
                ALStructureLimits.STRUCTURE_BLOCK_NAME_LENGTH,
                ALStructureLimits.STRUCTURE_BLOCK_DETECT_RANGE,
                nbt == null ? "无法验证" : state(nbt, runtimePhase),
                // Detect 要玩家按过按钮才置位，同样不算失败
                detect ? "OK" : "待首次使用");

        boolean hardFailure = !codec || Boolean.FALSE.equals(nbt) || (runtimePhase && !verify);
        if (hardFailure) {
            AbyssLibAtlas.LOGGER.warn(
                    "[AbyssLib/Atlas] 有注入点未生效（通常是原版类结构变化或被其它模组覆盖）。"
                            + "排查：确认 abysslib.mixins.json 已注册、搜索启动日志里的 mixin 报错、"
                            + "并核对目标常量是否仍在原版对应方法中。");
        }
    }

    /** 运行时阶段（世界数据包已加载）仍未置位的，就是真的没生效。 */
    private static String state(boolean flag, boolean runtimePhase) {
        if (flag) {
            return "OK";
        }
        return runtimePhase ? "未生效" : "待运行时";
    }

    /**
     * 真实回读：造一个结构方块实体，把 pos/size 设成上限值再走一遍 NBT 反序列化。
     *
     * @return true=放宽生效；false=仍被夹回原版 48；null=验证过程本身失败
     */
    private static Boolean verifyStructureBlockNbt() {
        try {
            StructureBlockEntity blockEntity = new StructureBlockEntity(
                    BlockPos.ZERO, Blocks.STRUCTURE_BLOCK.defaultBlockState());
            CompoundTag tag = new CompoundTag();
            tag.putInt("posX", -ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE);
            tag.putInt("sizeX", ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE);

            Method load = StructureBlockEntity.class.getDeclaredMethod(
                    "loadAdditional", CompoundTag.class, net.minecraft.core.HolderLookup.Provider.class);
            load.setAccessible(true);
            load.invoke(blockEntity, tag, RegistryAccess.EMPTY);

            return blockEntity.getStructureSize().getX() == ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE
                    && blockEntity.getStructurePos().getX() == -ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE;
        } catch (Throwable t) {
            AbyssLibAtlas.LOGGER.warn("[AbyssLib/Atlas] 结构方块 NBT 上限自检失败（不影响游戏）：{}", t.toString());
            return null;
        }
    }
}
