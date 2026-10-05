package com.altnoir.abysslib.datagen;

import com.altnoir.abysslib.model.AbyssLibReLink;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A fluent builder for one block's ReLink model definition.
 */
public final class ALModelDefinition {

    /**
     * 运行时扫描的定义目录（= mod id）。
     */
    public static final String DIRECTORY = AbyssLibReLink.NAMESPACE;

    /**
     * 本库内置的模型类型。
     */
    public enum Type {
        CTM("ctm"),
        CARPET_CTM("carpet_ctm"),
        PANE_CTM("pane_ctm"),
        GIANT("giant"),
        MURAL("mural"),
        PILLAR("pillar"),
        LIMITED_PILLAR("limited_pillar"),
        PANE_PILLAR("pane_pillar");

        private final String path;

        Type(String path) {
            this.path = path;
        }

        public Identifier id() {
            return Identifier.fromNamespaceAndPath(DIRECTORY, this.path);
        }
    }

    /**
     * 需要 `ctm_textures` 的「五连」类型。
     */
    private static final List<String> FIVE_TEXTURES = List.of("particle", "center", "empty", "vertical", "horizontal");

    private final Identifier blockId;
    private final Type type;
    private final JsonObject json = new JsonObject();
    private final Consumer<ALModelDefinition> saver;

    ALModelDefinition(Identifier blockId, Type type, Consumer<ALModelDefinition> saver) {
        this.blockId = blockId;
        this.type = type;
        this.saver = saver;
        this.json.addProperty(DIRECTORY + ":loader", type.id().toString());
    }

    public Identifier blockId() {
        return this.blockId;
    }

    public JsonObject toJson() {
        return this.json;
    }

    /**
     * 直接写一个 {@code ctm_textures} 键（完整控制）。
     */
    public ALModelDefinition texture(String key, Identifier texture) {
        texturesJson().addProperty(key, texture.toString());
        return this;
    }

    /**
     * 直接写一个 {@code ctm_textures} 键，字符串形式（可写 {@code "block/foo"}，自动补本模组命名空间）。
     */
    public ALModelDefinition texture(String key, String texture) {
        return texture(key, resolve(texture));
    }

    /**
     * 发光（本库扩展，见 README §3.1.1）。
     */
    public ALModelDefinition emissive() {
        this.json.addProperty(DIRECTORY + ":emissive", true);
        return this;
    }

    /**
     * 渲染层：{@code solid} / {@code cutout} / {@code cutout_mipped} / {@code translucent}。
     */
    public ALModelDefinition renderType(String renderType) {
        this.json.addProperty("render_type", renderType);
        return this;
    }

    /**
     * 颜色索引（走原版 BlockColor/ItemColor，生物群系染色用 0）。
     */
    public ALModelDefinition tint(int index) {
        this.json.addProperty("tint", index);
        return this;
    }

    /**
     * 固定颜色（ARGB）。
     */
    public ALModelDefinition tint(int r, int g, int b, int a) {
        var tint = new com.google.gson.JsonArray();
        tint.add(r);
        tint.add(g);
        tint.add(b);
        tint.add(a);
        this.json.add("tint", tint);
        return this;
    }

    /**
     * 逃生口：写任意顶层键（如 {@code connect_to} 条件树）。
     */
    public ALModelDefinition property(String key, JsonElement value) {
        this.json.add(key, value);
        return this;
    }

    /**
     * Derives the CTM set from a base texture; use {@link #ctmDir(String)} to override its directory.
     */
    public ALModelDefinition baseTexture(String baseTexture) {
        return baseTexture(resolve(baseTexture));
    }

    /**
     * 见 {@link #baseTexture(String)}。
     */
    public ALModelDefinition baseTexture(Identifier baseTexture) {
        final String path = baseTexture.getPath();
        final int slash = path.lastIndexOf('/');
        final String dir = slash < 0 ? "" : path.substring(0, slash);
        final String name = slash < 0 ? path : path.substring(slash + 1);
        final Identifier ctmDir = Identifier.fromNamespaceAndPath(
                baseTexture.getNamespace(), (dir.isEmpty() ? "" : dir + "/") + "ctm/" + name + "_ctm");
        return baseTexture(baseTexture, ctmDir);
    }

    /**
     * 用指定的 CTM 贴图目录（内含 {@code 0..3} 四张）+ 本体贴图作粒子。
     */
    public ALModelDefinition baseTexture(String baseTexture, String ctmDir) {
        return baseTexture(resolve(baseTexture), resolveDir(ctmDir));
    }

    /**
     * 见 {@link #baseTexture(String, String)}。
     */
    public ALModelDefinition baseTexture(Identifier baseTexture, Identifier ctmDir) {
        texture("particle", baseTexture);
        return ctmDir(ctmDir);
    }

    /**
     * 指定 CTM 四连目录（{@code 0=empty, 1=vertical, 2=horizontal, 3=center}）。
     */
    public ALModelDefinition ctmDir(String ctmDir) {
        return ctmDir(resolveDir(ctmDir));
    }

    /**
     * 见 {@link #ctmDir(String)}。
     */
    public ALModelDefinition ctmDir(Identifier ctmDir) {
        texture("empty", ctmDir.withSuffix("/0"));
        texture("vertical", ctmDir.withSuffix("/1"));
        texture("horizontal", ctmDir.withSuffix("/2"));
        texture("center", ctmDir.withSuffix("/3"));
        return this;
    }

    /**
     * 柱类（{@code pillar} / {@code limited_pillar} / {@code pane_pillar}）的五张贴图。
     */
    public ALModelDefinition pillarTextures(Identifier self, Identifier top, Identifier center, Identifier bottom, Identifier particle) {
        texture("self", self);
        texture("top", top);
        texture("center", center);
        texture("bottom", bottom);
        texture("particle", particle);
        return this;
    }

    /**
     * 柱类贴图，字符串形式。
     */
    public ALModelDefinition pillarTextures(String self, String top, String center, String bottom, String particle) {
        return pillarTextures(resolve(self), resolve(top), resolve(center), resolve(bottom), resolve(particle));
    }

    /**
     * 板类（{@code pane_ctm} / {@code pane_pillar}）可选的边缘贴图。
     */
    public ALModelDefinition paneEdges(Identifier edge, Identifier sideEdge) {
        texture("edge", edge);
        texture("side_edge", sideEdge);
        return this;
    }

    /**
     * 多格拼接（{@code giant} / {@code mural}）的尺寸。
     */
    public ALModelDefinition size(int width, int height) {
        this.json.addProperty("width", width);
        this.json.addProperty("height", height);
        return this;
    }

    /**
     * 多格拼接的编号贴图：{@code "1".."width*height"} 依次为 {@code <dir>/0, <dir>/1, ...}。
     */
    public ALModelDefinition numberedTextures(String dir) {
        return numberedTextures(resolve(dir));
    }

    /**
     * 见 {@link #numberedTextures(String)}。
     */
    public ALModelDefinition numberedTextures(Identifier dir) {
        final int width = this.json.has("width") ? this.json.get("width").getAsInt() : 0;
        final int height = this.json.has("height") ? this.json.get("height").getAsInt() : 0;
        if (width <= 0 || height <= 0) {
            throw new IllegalStateException("numberedTextures(...) 需要先调用 size(width, height)：" + this.blockId);
        }
        for (int i = 0; i < width * height; i++) {
            texture(Integer.toString(i + 1), dir.withSuffix("/" + i));
        }
        return this;
    }

    /**
     * 提交这份定义；缺少该类型必需字段时立刻抛错（避免生成出跑起来才报错的 JSON）。
     */
    public void save() {
        validate();
        saver.accept(this);
    }

    private void validate() {
        final Map<String, String> required = new LinkedHashMap<>();
        switch (type) {
            case CTM, CARPET_CTM, PANE_CTM -> FIVE_TEXTURES.forEach(key -> required.put(key, "ctm_textures"));
            case PILLAR, LIMITED_PILLAR, PANE_PILLAR -> List.of("particle", "self", "top", "center", "bottom")
                    .forEach(key -> required.put(key, "ctm_textures"));
            case GIANT, MURAL -> {
                required.put("width", "顶层字段");
                required.put("height", "顶层字段");
                required.put("particle", "ctm_textures");
                final int width = this.json.has("width") ? this.json.get("width").getAsInt() : 0;
                final int height = this.json.has("height") ? this.json.get("height").getAsInt() : 0;
                for (int i = 1; i <= Math.max(0, width * height); i++) {
                    required.put(Integer.toString(i), "ctm_textures");
                }
            }
        }

        final JsonObject ctmTextures = this.json.has("ctm_textures") ? this.json.getAsJsonObject("ctm_textures") : null;
        final List<String> missing = new ArrayList<>();
        required.forEach((key, where) -> {
            final boolean present = "顶层字段".equals(where)
                    ? this.json.has(key)
                    : ctmTextures != null && ctmTextures.has(key);
            if (!present) {
                missing.add(key + "（" + where + "）");
            }
        });
        if (!missing.isEmpty()) {
            throw new IllegalStateException("模型定义缺少必填字段 " + missing
                    + "：" + this.blockId + " / 类型 " + type.id());
        }
        if (ctmTextures != null && ctmTextures.isEmpty()) {
            throw new IllegalStateException("模型定义没有任何 ctm_textures：" + this.blockId);
        }
    }

    private JsonObject texturesJson() {
        if (this.json.has("ctm_textures")) {
            return this.json.getAsJsonObject("ctm_textures");
        }
        final JsonObject textures = new JsonObject();
        this.json.add("ctm_textures", textures);
        return textures;
    }

    /**
     * {@code "block/foo"} → {@code <本模组>:block/foo}；已带命名空间的原样返回。
     */
    private Identifier resolve(String path) {
        return path.indexOf(':') >= 0
                ? Identifier.parse(path)
                : Identifier.fromNamespaceAndPath(this.blockId.getNamespace(), path);
    }

    /**
     * 目录写法容错：允许写成 {@code "block/ctm/foo_ctm/"}。
     */
    private Identifier resolveDir(String path) {
        return resolve(path.endsWith("/") ? path.substring(0, path.length() - 1) : path);
    }
}
