package com.altnoir.abysslib.model.impl.loading;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.altnoir.abysslib.model.ALAthenaCompat;
import com.altnoir.abysslib.model.impl.client.DefaultModels;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 模型定义的存取：把 {@code assets/<任意命名空间>/<本库命名空间>/**.json} 里的定义读进内存，
 * 并负责「按类型取定义」与「取原始声明对象」两条路径。
 * <p>
 * 兼容层启用时（见 {@link ALAthenaCompat}）会一并扫描上游 Athena 的 {@code athena/} 目录，
 * 并接受 {@code "athena:loader"} 键。
 */
public class ALModelDefinitions {

    /** 本库的加载器声明键：{@code "relink:loader"}。 */
    public static final String LOADER_KEY = DefaultModels.NAMESPACE + ":loader";

    private static Function<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> getter = id -> null;
    private static final Map<ResourceLocation, JsonElement> data = new ConcurrentHashMap<>();

    public static void setGetter(Function<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> getter) {
        ALModelDefinitions.getter = Objects.requireNonNullElse(getter, id -> null);
    }

    public static void reload(ResourceManager manager) {
        ALModelDefinitions.data.clear();
        // 先扫上游 Athena 的目录，再扫本库的：同名条目以本库（较新）的写法为准。
        if (ALAthenaCompat.enabled()) {
            SimpleJsonResourceReloadListener.scanDirectory(
                    manager, ALAthenaCompat.ATHENA_NAMESPACE, new Gson(), ALModelDefinitions.data);
        }
        // 定义文件位于 assets/<任意命名空间>/relink/**.json（上游为 athena/ 目录）
        SimpleJsonResourceReloadListener.scanDirectory(
                manager, DefaultModels.NAMESPACE, new Gson(), ALModelDefinitions.data);
    }

    public static JsonObject getData(ResourceLocation modelType, ResourceLocation modelId) {
        var modelData = ALModelDefinitions.data.get(modelId);
        if (modelData != null) {
            return checkObject(modelType, modelData);
        }
        List<BlockStateModelLoader.LoadedJson> jsons = ALModelDefinitions.getter.apply(convertModelIdToBlockStatePath(modelId));
        if (jsons == null) return null;
        for (BlockStateModelLoader.LoadedJson json : jsons) {
            JsonObject object = checkObject(modelType, json.data());
            if (object != null) {
                return object;
            }
        }
        return null;
    }

    private static JsonObject checkObject(ResourceLocation modelType, JsonElement data) {
        if (data instanceof JsonObject object) {
            String type = getLoaderDeclaration(object);
            if (type != null && modelType.toString().equals(type)) {
                return object;
            }
        }
        return null;
    }

    /**
     * 读取模型 JSON 里的加载器声明：优先 {@code "relink:loader"}，
     * 兼容层启用时回退到上游 Athena 的 {@code "athena:loader"}。
     *
     * @return 声明的模型类型 id；两种键都没有时返回 {@code null}
     */
    public static String getLoaderDeclaration(JsonObject object) {
        if (object.has(LOADER_KEY)) {
            return GsonHelper.getAsString(object, LOADER_KEY, "");
        }
        if (ALAthenaCompat.enabled() && object.has(ALAthenaCompat.ATHENA_LOADER_KEY)) {
            return GsonHelper.getAsString(object, ALAthenaCompat.ATHENA_LOADER_KEY, "");
        }
        return null;
    }

    /**
     * 取该模型 id 的「原始声明对象」（不按类型过滤）：先查 {@code assets/<ns>/relink/**.json}
     * 定义目录，再回退到 blockstate 原文。
     * <p>
     * **注意**：同一个 blockstate 会被多个资源包叠加，{@code getter} 返回的是**列表**（原版 + 各模组/资源包各一份）。
     * 因此这里不是简单取第一个，而是**优先返回含本库键（{@code relink:*}，兼容期也含 {@code athena:*}）的那一份**，
     * 否则才退回第一个 —— 否则本库的覆盖会被原版那份盖掉（原版 JSON 里当然没有我们的键）。
     * <p>
     * **本库扩展**：用于「包裹原版模型发光」模式——blockstate 根上只写
     * {@code "relink:emissive": true}（没有 {@code relink:loader}）时，我们不改形状，
     * 只把原版已烘焙模型包一层全亮，见 {@code ALModelSetup.onModifyBakingResult}。
     */
    public static JsonObject getRawData(ResourceLocation modelId) {
        JsonElement modelData = ALModelDefinitions.data.get(modelId);
        if (modelData instanceof JsonObject object) {
            return object;
        }
        List<BlockStateModelLoader.LoadedJson> jsons = ALModelDefinitions.getter.apply(convertModelIdToBlockStatePath(modelId));
        if (jsons == null) return null;
        JsonObject first = null;
        for (BlockStateModelLoader.LoadedJson json : jsons) {
            if (json.data() instanceof JsonObject object) {
                if (first == null) {
                    first = object;
                }
                if (hasOurKeys(object)) {
                    return object;
                }
            }
        }
        return first;
    }

    /** {@return 该 JSON 是否含任意本库键（{@code relink:*}；兼容期也认 {@code athena:*}）} */
    private static boolean hasOurKeys(JsonObject object) {
        final String own = DefaultModels.NAMESPACE + ":";
        final boolean compat = ALAthenaCompat.enabled();
        final String legacy = ALAthenaCompat.ATHENA_NAMESPACE + ":";
        for (String key : object.keySet()) {
            if (key.startsWith(own)) {
                return true;
            }
            if (compat && key.startsWith(legacy)) {
                return true;
            }
        }
        return false;
    }

    private static ResourceLocation convertModelIdToBlockStatePath(ResourceLocation modelId) {
        return ResourceLocation.fromNamespaceAndPath(modelId.getNamespace(), "blockstates/" + modelId.getPath() + ".json");
    }
}
