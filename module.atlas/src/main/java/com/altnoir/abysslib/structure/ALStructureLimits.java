package com.altnoir.abysslib.structure;

// Atlas mixin 与启动自检共用的限制值。
public final class ALStructureLimits {

    private ALStructureLimits() {
    }

    public static final int VANILLA_STRUCTURE_BLOCK_LIMIT = 48;

    // 超过 127 时由数据包 mixin 附加扩展载荷。
    public static final int STRUCTURE_BLOCK_MAX_SIZE = 128;

    public static final int STRUCTURE_BLOCK_DETECT_RANGE = 128;

    public static final int STRUCTURE_BLOCK_NAME_LENGTH = 256;

    public static final int JIGSAW_MAX_DISTANCE_FROM_CENTER = 256;

    public static final int JIGSAW_MAX_DEPTH = 128;

    public static final int CORNER_SEARCH_CHECK_BUDGET = 2_000_000;

    // 目标代码运行后由对应 mixin 置位。

    public static volatile boolean structureBlockSizeApplied = false;

    public static volatile boolean detectRangeApplied = false;

    public static volatile boolean jigsawRangeApplied = false;

    public static volatile boolean jigsawVerifyApplied = false;

    public static volatile boolean placeJigsawApplied = false;
}
