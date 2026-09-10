package com.altnoir.abysslib.model.api.client.models;

import com.altnoir.abysslib.AbyssLib;
import com.altnoir.abysslib.model.api.client.utils.ALModelUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public class ALModelAttributes {

    /**
     * 发光开关的 JSON 键（值：布尔）。
     * <p>
     * **本库扩展**：上游 Athena（1.21.1 / wiki）没有发光/emissive 特性。
     * 开启后该模型的所有面强制 15/15 光照并关闭 AO 与方向性明暗，贴图颜色即所见，
     * 暗处看起来就是发光（走 NeoForge 的 baked lightmap：{@code FaceBakery} 写 quad 顶点光照，
     * 渲染时 {@code QuadLighter}/{@code applyBakedLighting} 取 max(烘焙光照, 世界光照)）。
     */
    public static final String EMISSIVE_KEY = AbyssLib.MOD_ID + ":emissive";

    public static final ALModelAttributes EMPTY = new ALModelAttributes(null, null);

    private final TintProvider tint;
    private final RenderType layer;
    private final boolean emissive;

    public ALModelAttributes(@Nullable TintProvider tint, @Nullable RenderType layer) {
        this(tint, layer, false);
    }

    public ALModelAttributes(@Nullable TintProvider tint, @Nullable RenderType layer, boolean emissive) {
        this.tint = tint;
        this.layer = layer;
        this.emissive = emissive;
    }

    public TintProvider getTint() {
        return this.tint;
    }

    public RenderType getLayer() {
        return this.layer;
    }

    /** {@return 是否发光（见 {@link #EMISSIVE_KEY}）} */
    public boolean isEmissive() {
        return this.emissive;
    }

    /**
     * {@return JSON 中是否出现了本类可解析的属性键}
     * <p>
     * 供 {@code DefaultModels} 判断是否需要为内置模型套属性装饰器（无属性键时不包装，零开销）。
     */
    public static boolean hasAttributes(JsonObject json) {
        return json.has("tint") || json.has("render_type") || json.has(EMISSIVE_KEY);
    }

    /** {@return 该 JSON 是否声明了发光（{@link #EMISSIVE_KEY}）} */
    public static boolean isEmissive(JsonObject json) {
        return GsonHelper.getAsBoolean(json, EMISSIVE_KEY, false);
    }

    @ApiStatus.Internal
    public static ALModelAttributes fromJson(JsonObject json) {
        var tint = TintProvider.fromJson(json);
        var layer = ALModelUtils.renderTypeFromJson(json);
        var emissive = GsonHelper.getAsBoolean(json, EMISSIVE_KEY, false);
        return new ALModelAttributes(tint, layer, emissive);
    }
}
