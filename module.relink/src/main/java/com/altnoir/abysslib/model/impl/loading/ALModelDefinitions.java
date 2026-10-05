package com.altnoir.abysslib.model.impl.loading;

import com.altnoir.abysslib.model.ALAthenaCompat;
import com.altnoir.abysslib.model.impl.client.DefaultModels;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public final class ALModelDefinitions extends SimplePreparableReloadListener<ALModelDefinitions.Data> {
    public static final Identifier RELOAD_ID = Identifier.fromNamespaceAndPath(DefaultModels.NAMESPACE, "model_definitions");
    public static final String LOADER_KEY = DefaultModels.NAMESPACE + ":loader";
    public static final ALModelDefinitions RELOAD_LISTENER = new ALModelDefinitions();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter BLOCKSTATES = FileToIdConverter.json("blockstates");
    private static volatile Data data = Data.EMPTY;

    private ALModelDefinitions() {
    }

    @Override
    protected Data prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> definitions = new HashMap<>();
        if (ALAthenaCompat.enabled()) {
            loadDirectory(manager, ALAthenaCompat.ATHENA_NAMESPACE, definitions);
        }
        loadDirectory(manager, DefaultModels.NAMESPACE, definitions);

        Map<Identifier, JsonObject> blockstates = new HashMap<>();
        BLOCKSTATES.listMatchingResourceStacks(manager).forEach((file, resources) -> {
            Identifier blockId = BLOCKSTATES.fileToId(file);
            JsonObject selected = null;
            for (Resource resource : resources) {
                JsonElement json = read(resource, file);
                if (json instanceof JsonObject object && hasReLinkData(object)) selected = object;
            }
            if (selected != null) blockstates.put(blockId, selected);
        });
        return new Data(Map.copyOf(definitions), Map.copyOf(blockstates));
    }

    @Override
    protected void apply(Data prepared, ResourceManager manager, ProfilerFiller profiler) {
        data = prepared;
    }

    public static JsonObject getData(Identifier modelType, Identifier modelId) {
        JsonObject result = checkObject(modelType, data.definitions().get(modelId));
        if (result == null) result = checkObject(modelType, data.definitions().get(blockModelId(modelId)));
        if (result != null) return result;
        return checkObject(modelType, data.blockstates().get(modelId));
    }

    public static JsonObject getRawData(Identifier modelId) {
        JsonElement definition = data.definitions().get(modelId);
        if (!(definition instanceof JsonObject)) definition = data.definitions().get(blockModelId(modelId));
        if (definition instanceof JsonObject object) return object;
        return data.blockstates().get(modelId);
    }

    private static Identifier blockModelId(Identifier blockId) {
        return blockId.getPath().startsWith("block/") ? blockId : blockId.withPath("block/" + blockId.getPath());
    }

    public static String getLoaderDeclaration(JsonObject object) {
        if (object.has(LOADER_KEY)) return GsonHelper.getAsString(object, LOADER_KEY, "");
        if (ALAthenaCompat.enabled() && object.has(ALAthenaCompat.ATHENA_LOADER_KEY)) {
            return GsonHelper.getAsString(object, ALAthenaCompat.ATHENA_LOADER_KEY, "");
        }
        return null;
    }

    private static JsonObject checkObject(Identifier modelType, JsonElement element) {
        if (element instanceof JsonObject object && modelType.toString().equals(getLoaderDeclaration(object)))
            return object;
        return null;
    }

    private static boolean hasReLinkData(JsonObject object) {
        String relinkPrefix = DefaultModels.NAMESPACE + ":";
        String athenaPrefix = ALAthenaCompat.ATHENA_NAMESPACE + ":";
        return object.keySet().stream().anyMatch(key -> key.startsWith(relinkPrefix)
                || ALAthenaCompat.enabled() && key.startsWith(athenaPrefix));
    }

    private static void loadDirectory(ResourceManager manager, String directory, Map<Identifier, JsonElement> result) {
        FileToIdConverter files = FileToIdConverter.json(directory);
        files.listMatchingResources(manager).forEach((file, resource) -> {
            JsonElement json = read(resource, file);
            if (json != null) result.put(files.fileToId(file), json);
        });
    }

    private static JsonElement read(Resource resource, Identifier id) {
        try (Reader reader = resource.openAsReader()) {
            return StrictJsonParser.parse(reader);
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to read ReLink definition {}", id, e);
            return null;
        }
    }

    record Data(Map<Identifier, JsonElement> definitions, Map<Identifier, JsonObject> blockstates) {
        private static final Data EMPTY = new Data(Map.of(), Map.of());
    }
}
