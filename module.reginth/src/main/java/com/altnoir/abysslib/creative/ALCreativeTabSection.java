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
 * 可选带一张<b>整行横幅贴图</b>（162×18，即创造栏一整行）：给了贴图的分区会由渲染器直接
 * 画这张图并<b>独占一整行</b>，且<b>不再叠加代码绘制的分区标题文字</b>——贴图自带标题/底纹，
 * 叠字会重影。不给贴图的分区退回标签页级 {@link ALBannerStyle}（纯色或内置 banner_N 贴图）
 * 并照常画标题文字，因此旧写法完全不受影响。
 */
public final class ALCreativeTabSection {
    /** 整行横幅贴图的像素宽度（= 创造栏 9 格 × 18px）。 */
    public static final int BANNER_TEXTURE_WIDTH = 162;
    /** 整行横幅贴图的像素高度（= 创造栏一行）。 */
    public static final int BANNER_TEXTURE_HEIGHT = 18;

    private final String translationKey;
    private final Component title;
    @Nullable
    private final ResourceLocation bannerTexture;
    private final List<Supplier<ItemStack>> entries = new ArrayList<>();

    /** 无横幅贴图：使用标签页级 {@link ALBannerStyle} 并绘制标题文字（旧行为）。 */
    public ALCreativeTabSection(String translationKey) {
        this(translationKey, null);
    }

    /**
     * 带整行横幅贴图（162×18）。贴图路径按普通资源路径写，例如
     * {@code ResourceLocation.fromNamespaceAndPath("mia", "textures/gui/ctab/building")}
     * （对应 {@code assets/mia/textures/gui/ctab/building.png}）。
     */
    public ALCreativeTabSection(String translationKey, @Nullable ResourceLocation bannerTexture) {
        this.translationKey = translationKey;
        this.title = Component.translatable(translationKey);
        this.bannerTexture = bannerTexture;
    }

    public String translationKey() {
        return translationKey;
    }

    public Component title() {
        return title;
    }

    /** 该分区的整行横幅贴图；为空表示用标签页级样式。 */
    public Optional<ResourceLocation> bannerTexture() {
        return Optional.ofNullable(bannerTexture);
    }

    /** 是否由贴图自带标题（有贴图即不自绘文字）。 */
    public boolean hasBannerTexture() {
        return bannerTexture != null;
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
