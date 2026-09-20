package com.altnoir.abysslib.creative;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 创造栏分区：一个标题 + 一组惰性物品条目。
 * 供 {@link ALSectionedCreativeModeTab} 使用；物品经 {@link #add} 加入后，
 * 标签页构建时会按分区顺序渲染（分区之间以空行分隔）。
 * <p>
 * 可选带一张<b>横幅贴图</b>（宽 = 格数 × 18px，高 18px）：给了贴图的分区由渲染器直接画这张图，
 * 横幅占该行行首 N 格、<b>左对齐</b>，物品从第 N+1 格<b>同行接续</b>排布
 * （N=9 即整行，物品从下一行开始，是原版式布局；N&lt;9 时横幅只占左边一段）。
 * 格数由构造器给出，见 {@link #ALCreativeTabSection(String, ResourceLocation, int)}。
 * <p>
 * 分区标题文字画在横幅最左侧，文字长于横幅时会被截断加省略号（不会压到右边物品上）。
 * 标题是否需要一层半透明底板见 {@link #titlePlate(ALTitlePlate)}，<b>默认不启用</b>。
 */
public final class ALCreativeTabSection {
    /**
     * 一格的像素宽度（创造栏每格 18px）。
     */
    public static final int UNIT_WIDTH = 18;
    /**
     * 横幅贴图的像素高度（= 创造栏一行）。
     */
    public static final int BANNER_TEXTURE_HEIGHT = 18;
    /**
     * 整行横幅贴图的像素宽度（= 创造栏 9 格 × 18px）。
     */
    public static final int BANNER_TEXTURE_WIDTH = ALBannerStyle.MAX_UNITS * UNIT_WIDTH;

    private final String translationKey;
    private final Component title;
    @Nullable
    private final ResourceLocation bannerTexture;
    /**
     * 横幅占的格数（1~9）；没有贴图时该值不参与版面计算。
     */
    private final int bannerUnits;
    private ALTitlePlate titlePlate = ALTitlePlate.DISABLED;
    private final List<Supplier<ItemStack>> entries = new ArrayList<>();

    /**
     * 无横幅贴图：使用标签页级 {@link ALBannerStyle} 并绘制标题文字（旧行为）。
     */
    public ALCreativeTabSection(String translationKey) {
        this(translationKey, null);
    }

    /**
     * 带横幅贴图，默认占<b>整行 9 格</b>。贴图路径按普通资源路径写，例如
     * {@code ResourceLocation.fromNamespaceAndPath("mia", "textures/gui/ctab/building")}
     * （对应 {@code assets/mia/textures/gui/ctab/building.png}）。
     * <p>
     * 路径带不带 {@code .png} 都可以：渲染器会统一补成 MC 资产加载要求的带扩展名形式
     * （非图集贴图走 {@code SimpleTexture}，它不会自己补 {@code .png}）。
     */
    public ALCreativeTabSection(String translationKey, @Nullable ResourceLocation bannerTexture) {
        this(translationKey, bannerTexture, ALBannerStyle.MAX_UNITS);
    }

    /**
     * 带横幅贴图并指定横幅占几格（1~9，左对齐；9 即整行）。
     * <p>
     * 贴图会被拉伸到 {@code bannerUnits × 18} 像素宽绘制，所以贴图宽度按
     * {@code bannerUnits × 18} 做最清楚（例如 4 格 → 72×18）；给 64×18 这类非整倍数宽度也能用，
     * 只是会被轻微拉伸。
     */
    public ALCreativeTabSection(String translationKey, @Nullable ResourceLocation bannerTexture, int bannerUnits) {
        this.translationKey = translationKey;
        this.title = Component.translatable(translationKey);
        this.bannerTexture = bannerTexture;
        this.bannerUnits = ALBannerStyle.validateUnits(bannerUnits);
    }

    public String translationKey() {
        return translationKey;
    }

    public Component title() {
        return title;
    }

    /**
     * 该分区的横幅贴图；为空表示用标签页级样式。
     */
    public Optional<ResourceLocation> bannerTexture() {
        return Optional.ofNullable(bannerTexture);
    }

    /**
     * 该分区是否有自带横幅贴图（有则渲染器画贴图、不画标签页级横幅）。
     */
    public boolean hasBannerTexture() {
        return bannerTexture != null;
    }

    /**
     * 横幅占的格数（1~9）。
     */
    public int bannerUnits() {
        return bannerUnits;
    }

    /**
     * 横幅的像素宽度 = {@code bannerUnits × 18}。
     */
    public int bannerPixelWidth() {
        return bannerUnits * UNIT_WIDTH;
    }

    /**
     * 标题底板（{@link ALTitlePlate}）；默认 {@link ALTitlePlate#DISABLED 不启用}。
     */
    public ALTitlePlate titlePlate() {
        return titlePlate;
    }

    /**
     * 设置标题底板；传 {@link ALTitlePlate#DISABLED} 可关掉。
     */
    public ALCreativeTabSection titlePlate(ALTitlePlate titlePlate) {
        this.titlePlate = titlePlate == null ? ALTitlePlate.DISABLED : titlePlate;
        return this;
    }

    public void add(ItemLike item) {
        add(() -> item.asItem().getDefaultInstance());
    }

    public void add(ItemStack stack) {
        add(stack::copy);
    }

    public void add(Supplier<ItemStack> stack) {
        entries.add(stack);
    }

    public void clear() {
        entries.clear();
    }

    public List<ItemStack> itemStacks() {
        return entries.stream()
                .map(Supplier::get)
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }
}
