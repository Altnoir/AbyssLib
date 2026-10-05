package com.altnoir.abysslib.datagen;

import com.altnoir.abysslib.reginth.util.entry.BlockEntry;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generates ReLink model definitions under {@code assets/<namespace>/relink/}.
 */
public abstract class ALModelDefinitionProvider implements DataProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final PackOutput.PathProvider pathProvider;
    private final String modId;
    private final Map<Identifier, ALModelDefinition> definitions = new LinkedHashMap<>();

    protected ALModelDefinitionProvider(PackOutput output, String modId) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, ALModelDefinition.DIRECTORY);
        this.modId = modId;
    }

    /**
     * Declare definitions with methods such as {@code ctm(...)} and {@code pillar(...)}.
     */
    protected abstract void registerDefinitions();

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        definitions.clear();

        registerDefinitions();

        return CompletableFuture.allOf(definitions.values().stream()
                .map(definition -> DataProvider.saveStable(output, definition.toJson(),
                        pathProvider.json(definition.blockId())))
                .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "AbyssLib model definitions (" + modId + ")";
    }

    public ALModelDefinition ctm(Block block) {
        return definition(block, ALModelDefinition.Type.CTM);
    }

    public ALModelDefinition ctm(BlockEntry<? extends Block> block) {
        return ctm(block.get());
    }

    public ALModelDefinition carpetCtm(Block block) {
        return definition(block, ALModelDefinition.Type.CARPET_CTM);
    }

    public ALModelDefinition carpetCtm(BlockEntry<? extends Block> block) {
        return carpetCtm(block.get());
    }

    public ALModelDefinition paneCtm(Block block) {
        return definition(block, ALModelDefinition.Type.PANE_CTM);
    }

    public ALModelDefinition paneCtm(BlockEntry<? extends Block> block) {
        return paneCtm(block.get());
    }

    public ALModelDefinition giant(Block block) {
        return definition(block, ALModelDefinition.Type.GIANT);
    }

    public ALModelDefinition giant(BlockEntry<? extends Block> block) {
        return giant(block.get());
    }

    public ALModelDefinition mural(Block block) {
        return definition(block, ALModelDefinition.Type.MURAL);
    }

    public ALModelDefinition mural(BlockEntry<? extends Block> block) {
        return mural(block.get());
    }

    public ALModelDefinition pillar(Block block) {
        return definition(block, ALModelDefinition.Type.PILLAR);
    }

    public ALModelDefinition pillar(BlockEntry<? extends Block> block) {
        return pillar(block.get());
    }

    public ALModelDefinition limitedPillar(Block block) {
        return definition(block, ALModelDefinition.Type.LIMITED_PILLAR);
    }

    public ALModelDefinition limitedPillar(BlockEntry<? extends Block> block) {
        return limitedPillar(block.get());
    }

    public ALModelDefinition panePillar(Block block) {
        return definition(block, ALModelDefinition.Type.PANE_PILLAR);
    }

    public ALModelDefinition panePillar(BlockEntry<? extends Block> block) {
        return panePillar(block.get());
    }

    /**
     * 任意类型 + 任意方块 id（给非注册方块 / 数据包场景留的口子）。
     */
    public ALModelDefinition definition(Identifier blockId, ALModelDefinition.Type type) {
        return new ALModelDefinition(blockId, type, this::add);
    }

    private ALModelDefinition definition(Block block, ALModelDefinition.Type type) {
        return definition(BuiltInRegistries.BLOCK.getKey(block), type);
    }

    private void add(ALModelDefinition definition) {
        final ALModelDefinition previous = definitions.put(definition.blockId(), definition);
        if (previous != null) {
            LOGGER.warn("AbyssLib/ReLink: 模型定义重复声明，后者覆盖前者：{}", definition.blockId());
        }
    }
}
