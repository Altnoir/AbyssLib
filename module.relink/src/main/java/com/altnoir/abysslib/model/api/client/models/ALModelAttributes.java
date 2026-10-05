package com.altnoir.abysslib.model.api.client.models;

import com.altnoir.abysslib.model.AbyssLibReLink;
import com.altnoir.abysslib.model.api.client.utils.ALModelUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public record ALModelAttributes(TintProvider tint, ChunkSectionLayer layer, boolean emissive) {

    /**
     * JSON boolean that disables shading and sets maximum block light.
     */
    public static final String EMISSIVE_KEY = AbyssLibReLink.NAMESPACE + ":emissive";

    public static final ALModelAttributes EMPTY = new ALModelAttributes(null, null);

    public ALModelAttributes(@Nullable TintProvider tint, @Nullable ChunkSectionLayer layer) {
        this(tint, layer, false);
    }

    public ALModelAttributes(@Nullable TintProvider tint, @Nullable ChunkSectionLayer layer, boolean emissive) {
        this.tint = tint;
        this.layer = layer;
        this.emissive = emissive;
    }

    /**
     * {@return 是否发光（见 {@link #EMISSIVE_KEY}）}
     */
    @Override
    public boolean emissive() {
        return this.emissive;
    }

    /**
     * Whether the JSON declares any shared model attributes.
     */
    public static boolean hasAttributes(JsonObject json) {
        return json.has("tint") || json.has("render_type") || json.has(EMISSIVE_KEY);
    }

    /**
     * {@return 该 JSON 是否声明了发光（{@link #EMISSIVE_KEY}）}
     */
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
