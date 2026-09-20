package com.altnoir.abysslib.creative;

/**
 * 分区标题的底板（可选，<b>默认不启用</b>）。
 * <p>
 * 横幅贴图往往是有渐变/花纹的，白字直接压上去在浅色区域会看不清；给标题垫一层半透明底色
 * （通常是黑色）就能保证可读。但底板会盖掉贴图本身的一部分，所以本库<b>不默认开</b>，
 * 由使用方按分区决定要不要，以及用什么颜色/多不透明。
 * <p>
 * 用 {@link ALCreativeTabSection#titlePlate(ALTitlePlate)} 挂到某个分区上；
 * 只对画在横幅上的标题生效（{@code ALSectionedCreativeTabRenderer} 负责绘制）。
 * <p>
 * 用法：
 * <pre>{@code
 * // 不启用（默认）
 * section.titlePlate(ALTitlePlate.DISABLED);
 *
 * // 25% 黑底
 * section.titlePlate(ALTitlePlate.argb(0x40000000));
 *
 * // RGB + 不透明度（0~1）
 * section.titlePlate(ALTitlePlate.of(0x000000, 0.25F));
 *
 * // 蓝色 40%，左右留白 3px、上下 1px
 * section.titlePlate(ALTitlePlate.of(0x102040, 0.4F).withPadding(3, 1));
 * }</pre>
 *
 * @param enabled   是否绘制底板
 * @param argbColor 底板颜色，ARGB（高 8 位是不透明度，{@code 0xFF000000} 为不透明黑）
 * @param padX      底板在文字左右各多出的像素
 * @param padY      底板在文字上下各多出的像素
 */
public record ALTitlePlate(boolean enabled, int argbColor, int padX, int padY) {

    /**
     * 默认值：不启用（此时颜色/留白只是占位，不参与绘制）。
     */
    public static final ALTitlePlate DISABLED = new ALTitlePlate(false, 0x40000000, 2, 2);

    /**
     * 不透明度的最大/最小合法值（0~255）。
     */
    public static final int MIN_ALPHA = 0;
    public static final int MAX_ALPHA = 255;

    public ALTitlePlate {
        padX = Math.max(0, padX);
        padY = Math.max(0, padY);
    }

    // ---------- 构造 ----------

    /**
     * 用 ARGB 颜色启用（{@code 0x40000000} = 25% 黑）。
     */
    public static ALTitlePlate argb(int argbColor) {
        return new ALTitlePlate(true, argbColor, DISABLED.padX(), DISABLED.padY());
    }

    /**
     * 用 ARGB 颜色 + 自定义留白启用。
     */
    public static ALTitlePlate argb(int argbColor, int padX, int padY) {
        return new ALTitlePlate(true, argbColor, padX, padY);
    }

    /**
     * 用 RGB 颜色 + 不透明度启用。
     *
     * @param rgb     颜色，形如 {@code 0x102040}（不含 alpha，高 8 位会被忽略）
     * @param opacity 不透明度，0（全透明）~ 1（不透明），越界会被夹到范围内
     */
    public static ALTitlePlate of(int rgb, float opacity) {
        return new ALTitlePlate(true, withAlpha(rgb, opacity), DISABLED.padX(), DISABLED.padY());
    }

    // ---------- 调参（返回副本，record 本身不可变） ----------

    /**
     * 换颜色（含 alpha），保持启用状态与留白。
     */
    public ALTitlePlate withColor(int argbColor) {
        return new ALTitlePlate(enabled, argbColor, padX, padY);
    }

    /**
     * 只改不透明度，颜色（RGB）不变。
     */
    public ALTitlePlate withOpacity(float opacity) {
        return new ALTitlePlate(enabled, withAlpha(argbColor & 0xFFFFFF, opacity), padX, padY);
    }

    /**
     * 只改不透明度（0~255）。
     */
    public ALTitlePlate withAlpha(int alpha) {
        int a = Math.max(MIN_ALPHA, Math.min(MAX_ALPHA, alpha));
        return new ALTitlePlate(enabled, (a << 24) | (argbColor & 0xFFFFFF), padX, padY);
    }

    /**
     * 只改留白。
     */
    public ALTitlePlate withPadding(int padX, int padY) {
        return new ALTitlePlate(enabled, argbColor, padX, padY);
    }

    /**
     * 启用/停用。
     */
    public ALTitlePlate withEnabled(boolean enabled) {
        return new ALTitlePlate(enabled, argbColor, padX, padY);
    }

    // ---------- 取值 ----------

    /**
     * 当前不透明度（0~255）。
     */
    public int alpha() {
        return (argbColor >>> 24) & 0xFF;
    }

    /**
     * 当前不透明度（0~1）。
     */
    public float opacity() {
        return alpha() / (float) MAX_ALPHA;
    }

    /**
     * 当前颜色（不含 alpha）。
     */
    public int rgb() {
        return argbColor & 0xFFFFFF;
    }

    /**
     * 拼接颜色 + 不透明度；{@code opacity} 会被夹到 0~1。
     */
    private static int withAlpha(int rgb, float opacity) {
        float clamped = Math.max(0.0F, Math.min(1.0F, opacity));
        int a = Math.round(clamped * MAX_ALPHA);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
