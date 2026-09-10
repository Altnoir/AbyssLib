package com.altnoir.abysslib.model.impl.loading;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

public class ALModelDefinitions {

    private static Function<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> getter = id -> null;
    private static final Map<ResourceLocation, JsonElement> data = new ConcurrentHashMap<>();

    public static void setGetter(Function<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> getter) {
        ALModelDefinitions.getter = Objects.requireNonNullElse(getter, id -> null);
    }

    public static void reload(ResourceManager manager) {
        ALModelDefinitions.data.clear();
        // 定义文件位于 assets/<任意命名空间>/abysslib/**.json（上游为 athena/ 目录）
        SimpleJsonResourceReloadListener.scanDirectory(manager, DefaultModels.MODID, new Gson(), ALModelDefinitions.data);
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
            String type = GsonHelper.getAsString(object, DefaultModels.MODID + ":loader", "");
            if (modelType.toString().equals(type)) {
                return object;
            }
        }
        return null;
    }

    /**
     * 取该模型 id 的「原始声明对象」（不按类型过滤）：先查 {@code assets/<ns>/abysslib/**.json}
     * 定义目录，再回退到 blockstate 原文。
     * <p>
     * **注意**：同一个 blockstate 会被多个资源包叠加，{@code getter} 返回的是**列表**（原版 + 各模组/资源包各一份）。
     * 因此这里不是简单取第一个，而是**优先返回含 {@code abysslib:*} 键的那一份**，否则才退回第一个
     * —— 否则本库的覆盖会被原版那份盖掉（原版 JSON 里当然没有我们的键）。
     * <p>
     * **本库扩展**：用于「包裹原版模型发光」模式——blockstate 根上只写
     * {@code "abysslib:emissive": true}（没有 {@code abysslib:loader}）时，我们不改形状，
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

    /** {@return 该 JSON 是否含任意 {@code <modid>:*} 键（即本库会读取的声明）} */
    private static boolean hasOurKeys(JsonObject object) {
        final String prefix = DefaultModels.MODID + ":";
        for (String key : object.keySet()) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static ResourceLocation convertModelIdToBlockStatePath(ResourceLocation modelId) {
        return ResourceLocation.fromNamespaceAndPath(modelId.getNamespace(), "blockstates/" + modelId.getPath() + ".json");
    }
}
