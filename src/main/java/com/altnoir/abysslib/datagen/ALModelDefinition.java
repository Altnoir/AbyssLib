package com.altnoir.abysslib.datagen;

import com.altnoir.abysslib.AbyssLib;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 一个方块模型的「模型定义」（对应运行时 {@code assets/<modid>/abysslib/<方块>.json}）。
 * <p>
 * 用法：在 {@link ALModelDefinitionProvider} 子类里链式声明后 {@link #save()}：
 * <pre>{@code
 * ctm(PoBlocks.GLOWING_ORE).baseTexture("block/glowing_ore").emissive().save();
 * }</pre>
 * 生成的就是运行时会读到的那份定义 JSON：键 {@code "abysslib:loader"} = 模型类型，
 * 其余字段（{@code ctm_textures} / {@code width} / {@code height} / {@code abysslib:emissive} …）与手写一致。
 * 详见 README「内置模型加载器」一节。
 */
public final class ALModelDefinition {

    /** 运行时扫描的定义目录（= mod id）。 */
    public static final String DIRECTORY = AbyssLib.MOD_ID;

    /** 本库内置的模型类型。 */
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

        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath(DIRECTORY, this.path);
        }
    }

    /** 需要 `ctm_textures` 的「五连」类型。 */
    private static final List<String> FIVE_TEXTURES = List.of("particle", "center", "empty", "vertical", "horizontal");

    private final ResourceLocation blockId;
    private final Type type;
    private final JsonObject json = new JsonObject();
    private final Set<ResourceLocation> textures = new LinkedHashSet<>();
    private final Consumer<ALModelDefinition> saver;

    ALModelDefinition(ResourceLocation blockId, Type type, Consumer<ALModelDefinition> saver) {
        this.blockId = blockId;
        this.type = type;
        this.saver = saver;
        this.json.addProperty(DIRECTORY + ":loader", type.id().toString());
    }

    public ResourceLocation blockId() {
        return this.blockId;
    }

    public JsonObject toJson() {
        return this.json;
    }

    /** 本定义引用到的贴图（供生成时做存在性提醒）。 */
    public Set<ResourceLocation> textures() {
        return this.textures;
    }

    // ------------------------------------------------------------------
    // 通用设置
    // ------------------------------------------------------------------

    /** 直接写一个 {@code ctm_textures} 键（完整控制）。 */
    public ALModelDefinition texture(String key, ResourceLocation texture) {
        texturesJson().addProperty(key, texture.toString());
        this.textures.add(texture);
        return this;
    }

    /** 直接写一个 {@code ctm_textures} 键，字符串形式（可写 {@code "block/foo"}，自动补本模组命名空间）。 */
    public ALModelDefinition texture(String key, String texture) {
        return texture(key, resolve(texture));
    }

    /** 发光（本库扩展，见 README §3.1.1）。 */
    public ALModelDefinition emissive() {
        this.json.addProperty(DIRECTORY + ":emissive", true);
        return this;
    }

    /** 渲染层：{@code solid} / {@code cutout} / {@code cutout_mipped} / {@code translucent}。 */
    public ALModelDefinition renderType(String renderType) {
        this.json.addProperty("render_type", renderType);
        return this;
    }

    /** 颜色索引（走原版 BlockColor/ItemColor，生物群系染色用 0）。 */
    public ALModelDefinition tint(int index) {
        this.json.addProperty("tint", index);
        return this;
    }

    /** 固定颜色（ARGB）。 */
    public ALModelDefinition tint(int r, int g, int b, int a) {
        var tint = new com.google.gson.JsonArray();
        tint.add(r);
        tint.add(g);
        tint.add(b);
        tint.add(a);
        this.json.add("tint", tint);
        return this;
    }

    /** 逃生口：写任意顶层键（如 {@code connect_to} 条件树）。 */
    public ALModelDefinition property(String key, JsonElement value) {
        this.json.add(key, value);
        return this;
    }

    // ------------------------------------------------------------------
    // 各类型便利方法
    // ------------------------------------------------------------------

    /**
     * CTM 系列（{@code ctm} / {@code carpet_ctm} / {@code pane_ctm}）：由**本体贴图**推导五张贴图。
     * <p>
     * 约定（与常见 CTM 材质包一致，索引对应 wiki 示例）：
     * <ul>
     *     <li>{@code particle} = 本体贴图，如 {@code mymod:block/foo}</li>
     *     <li>{@code empty} = {@code <dir>/ctm/<name>_ctm/0}</li>
     *     <li>{@code vertical} = {@code <dir>/ctm/<name>_ctm/1}</li>
     *     <li>{@code horizontal} = {@code <dir>/ctm/<name>_ctm/2}</li>
     *     <li>{@code center} = {@code <dir>/ctm/<name>_ctm/3}</li>
     * </ul>
     * 其中 {@code <dir>/<name>} 是本体贴图的目录与文件名。CTM 目录可用 {@link #ctmDir(String)} 覆盖，
     * 或用 {@link #texture(String, String)} 逐张指定。
     */
    public ALModelDefinition baseTexture(String baseTexture) {
        return baseTexture(resolve(baseTexture));
    }

    /** 见 {@link #baseTexture(String)}。 */
    public ALModelDefinition baseTexture(ResourceLocation baseTexture) {
        final String path = baseTexture.getPath();
        final int slash = path.lastIndexOf('/');
        final String dir = slash < 0 ? "" : path.substring(0, slash);
        final String name = slash < 0 ? path : path.substring(slash + 1);
        final ResourceLocation ctmDir = ResourceLocation.fromNamespaceAndPath(
                baseTexture.getNamespace(), (dir.isEmpty() ? "" : dir + "/") + "ctm/" + name + "_ctm");
        return baseTexture(baseTexture, ctmDir);
    }

    /** 用指定的 CTM 贴图目录（内含 {@code 0..3} 四张）+ 本体贴图作粒子。 */
    public ALModelDefinition baseTexture(String baseTexture, String ctmDir) {
        return baseTexture(resolve(baseTexture), resolveDir(ctmDir));
    }

    /** 见 {@link #baseTexture(String, String)}。 */
    public ALModelDefinition baseTexture(ResourceLocation baseTexture, ResourceLocation ctmDir) {
        texture("particle", baseTexture);
        return ctmDir(ctmDir);
    }

    /** 指定 CTM 四连目录（{@code 0=empty, 1=vertical, 2=horizontal, 3=center}）。 */
    public ALModelDefinition ctmDir(String ctmDir) {
        return ctmDir(resolveDir(ctmDir));
    }

    /** 见 {@link #ctmDir(String)}。 */
    public ALModelDefinition ctmDir(ResourceLocation ctmDir) {
        texture("empty", ctmDir.withSuffix("/0"));
        texture("vertical", ctmDir.withSuffix("/1"));
        texture("horizontal", ctmDir.withSuffix("/2"));
        texture("center", ctmDir.withSuffix("/3"));
        return this;
    }

    /** 柱类（{@code pillar} / {@code limited_pillar} / {@code pane_pillar}）的五张贴图。 */
    public ALModelDefinition pillarTextures(ResourceLocation self, ResourceLocation top, ResourceLocation center, ResourceLocation bottom, ResourceLocation particle) {
        texture("self", self);
        texture("top", top);
        texture("center", center);
        texture("bottom", bottom);
        texture("particle", particle);
        return this;
    }

    /** 柱类贴图，字符串形式。 */
    public ALModelDefinition pillarTextures(String self, String top, String center, String bottom, String particle) {
        return pillarTextures(resolve(self), resolve(top), resolve(center), resolve(bottom), resolve(particle));
    }

    /** 板类（{@code pane_ctm} / {@code pane_pillar}）可选的边缘贴图。 */
    public ALModelDefinition paneEdges(ResourceLocation edge, ResourceLocation sideEdge) {
        texture("edge", edge);
        texture("side_edge", sideEdge);
        return this;
    }

    /** 多格拼接（{@code giant} / {@code mural}）的尺寸。 */
    public ALModelDefinition size(int width, int height) {
        this.json.addProperty("width", width);
        this.json.addProperty("height", height);
        return this;
    }

    /** 多格拼接的编号贴图：{@code "1".."width*height"} 依次为 {@code <dir>/0, <dir>/1, ...}。 */
    public ALModelDefinition numberedTextures(String dir) {
        return numberedTextures(resolve(dir));
    }

    /** 见 {@link #numberedTextures(String)}。 */
    public ALModelDefinition numberedTextures(ResourceLocation dir) {
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

    // ------------------------------------------------------------------
    // 保存与校验
    // ------------------------------------------------------------------

    /** 提交这份定义；缺少该类型必需字段时立刻抛错（避免生成出跑起来才报错的 JSON）。 */
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

    /** {@code "block/foo"} → {@code <本模组>:block/foo}；已带命名空间的原样返回。 */
    private ResourceLocation resolve(String path) {
        return path.indexOf(':') >= 0
                ? ResourceLocation.parse(path)
                : ResourceLocation.fromNamespaceAndPath(this.blockId.getNamespace(), path);
    }

    /** 目录写法容错：允许写成 {@code "block/ctm/foo_ctm/"}。 */
    private ResourceLocation resolveDir(String path) {
        return resolve(path.endsWith("/") ? path.substring(0, path.length() - 1) : path);
    }
}
