package com.altnoir.abysslib.datagen;

import com.mojang.logging.LogUtils;
import com.altnoir.abysslib.reginth.util.entry.BlockEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 把「本库 CTM / 动态模型定义」纳入 datagen：生成运行时会读取的模型定义文件
 * {@code assets/<modid>/abysslib/<方块>.json}。
 * <p>
 * 为什么是这个文件而不是 blockstate：本库的 CTM 支持两种写法，其中**定义目录**
 * （{@code assets/<modid>/abysslib/<方块>.json}）不需要接管 blockstate，
 * 因此可以和现有 {@code RegistrateBlockstateProvider} 的模型/物品模型生成**共存**、互不覆盖。
 * blockstate 照常生成（它同时充当"没有本库模型加载器时的回退外观"）：
 * <pre>{@code
 * // 1) 常规模型 + blockstate + 物品模型（用你现有的 helper）
 * simpleBlockWithItem(MyBlocks.GLOWING_ORE.get(), models().cubeAll("glowing_ore", modLoc("block/glowing_ore")));
 * }</pre>
 * <pre>{@code
 * // 2) CTM 定义（本 provider）
 * public class CtmModelGen extends ALModelDefinitionProvider {
 *     public CtmModelGen(PackOutput output, ExistingFileHelper helper) {
 *         super(output, MyMod.MOD_ID, helper);
 *     }
 *
 *     @Override
 *     protected void registerDefinitions() {
 *         ctm(MyBlocks.GLOWING_ORE).baseTexture("block/glowing_ore").emissive().save();
 *     }
 * }
 *
 * // 3) 挂到 GatherDataEvent（客户端资源）
 * generators.addProvider(event.includeClient(), new CtmModelGen(packOutput, existingFileHelper));
 * }</pre>
 * 生成时会检查引用到的贴图是否存在于已知资源里，缺图会汇总成一条 WARN（不阻断生成，
 * 因为贴图也可能来自资源包/其它模组）。字段缺失则直接抛错，避免生成出运行期才失败的 JSON。
 */
public abstract class ALModelDefinitionProvider implements DataProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final PackOutput.PathProvider pathProvider;
    private final String modId;
    private final ExistingFileHelper existingFileHelper;

    private final Map<ResourceLocation, ALModelDefinition> definitions = new LinkedHashMap<>();
    private final Set<ResourceLocation> referencedTextures = new LinkedHashSet<>();

    protected ALModelDefinitionProvider(PackOutput output, String modId, ExistingFileHelper existingFileHelper) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, ALModelDefinition.DIRECTORY);
        this.modId = modId;
        this.existingFileHelper = existingFileHelper;
    }

    /** 子类在这里用 {@code ctm(...)} / {@code pillar(...)} 等声明模型定义。 */
    protected abstract void registerDefinitions();

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        definitions.clear();
        referencedTextures.clear();

        registerDefinitions();

        checkTextures();

        return CompletableFuture.allOf(definitions.values().stream()
                .map(definition -> DataProvider.saveStable(output, definition.toJson(),
                        pathProvider.json(definition.blockId())))
                .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "AbyssLib model definitions (" + modId + ")";
    }

    // ------------------------------------------------------------------
    // 类型入口（Block / Registrate BlockEntry 两种写法）
    // ------------------------------------------------------------------

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

    /** 任意类型 + 任意方块 id（给非注册方块 / 数据包场景留的口子）。 */
    public ALModelDefinition definition(ResourceLocation blockId, ALModelDefinition.Type type) {
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
        referencedTextures.addAll(definition.textures());
    }

    /** 汇总检查引用到的贴图是否存在（缺图只提示，不阻断）。 */
    private void checkTextures() {
        if (existingFileHelper == null || referencedTextures.isEmpty()) {
            return;
        }
        final List<ResourceLocation> missing = new ArrayList<>();
        for (ResourceLocation texture : referencedTextures) {
            if (!existingFileHelper.exists(texture, PackType.CLIENT_RESOURCES, ".png", "textures")) {
                missing.add(texture);
            }
        }
        if (!missing.isEmpty()) {
            missing.sort(Comparator.comparing(ResourceLocation::toString));
            LOGGER.warn("AbyssLib/ReLink: 以下 CTM 贴图在已知资源里找不到（可能还没画，或来自资源包/其它模组）：{}",
                    missing.stream().map(ResourceLocation::toString).toList());
        }
    }
}
