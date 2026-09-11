package com.altnoir.abysslib.structure;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * <b>一键助手</b>：把「grid profile + {@code atlas:jigsaw} 结构 + 配套 structure_set」三条 datagen 注册
 * 收在一个对象里，并<b>由 profile 自动派生 structure_set</b>，从而让"placement 与结构必须引用同一个 profile"
 * 这件事不可能写错（这是使用本套功能时最容易出的错）。
 *
 * <h2>用法（放在消费方模组入口）</h2>
 * <pre>{@code
 * public MyMod(IEventBus modBus, ModContainer container) {
 *     ALStructureDatagen.create()
 *         .profile(MyProfiles.ROAD, ALGridProfile.forRadius(256, 64, 10387312))
 *         .jigsaw(MyStructures.ROAD, MyStructureSets.ROADS, MyProfiles.ROAD,
 *                 (biomes, pools, profile) -> new ALJigsawStructure(
 *                         new Structure.StructureSettings.Builder(biomes.getOrThrow(MyTags.HAS_ROAD))
 *                                 .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
 *                                 .terrainAdapation(TerrainAdjustment.NONE).build(),
 *                         pools.getOrThrow(MyPools.ROAD_START),
 *                         32, ConstantHeight.of(VerticalAnchor.absolute(0)), false,
 *                         Heightmap.Types.WORLD_SURFACE_WG, 256, profile))
 *         .register(MyMod.reginth());
 * }
 * }</pre>
 *
 * <p>不想用这个助手也完全可以：直接往 {@code reginth().getDataGenInitializer()}
 * 里 {@code add(ALGridProfile.KEY / Registries.STRUCTURE / Registries.STRUCTURE_SET, ...)} 即可，
 * 两种写法产物完全一致（见 README §9.2 / §9.3）。
 */
public final class ALStructureDatagen {

    /** 结构工厂：bootstrap 期间拿不到 Holder 的绑定值，所以这里只给查找器（见 README §9.2 的 ⚠️）。 */
    @FunctionalInterface
    public interface JigsawFactory {
        ALJigsawStructure create(
                HolderGetter<Biome> biomes,
                HolderGetter<StructureTemplatePool> pools,
                Holder<ALGridProfile> profile);
    }

    /** 结构集工厂（给需要自定义 placement 的场合；用 {@link #jigsaw} 时不需要写）。 */
    @FunctionalInterface
    public interface SetFactory {
        StructureSet create(HolderGetter<Structure> structures, HolderGetter<ALGridProfile> profiles);
    }

    /**
     * 结构条目：`atlas:jigsaw` 一定要有 profile，而 bootstrap 期间只能拿到 {@code HolderGetter}
     * （绑定值不能取，见 README §9.2 的 ⚠️），所以把 profile 的 key 一起存下来，在 bootstrap 里再解析。
     */
    private record StructureEntry(ResourceKey<ALGridProfile> profileKey, JigsawFactory factory) {
    }

    private final Map<ResourceKey<ALGridProfile>, ALGridProfile> profiles = new LinkedHashMap<>();
    private final Map<ResourceKey<Structure>, StructureEntry> structures = new LinkedHashMap<>();
    private final Map<ResourceKey<StructureSet>, SetFactory> sets = new LinkedHashMap<>();

    private ALStructureDatagen() {
    }

    public static ALStructureDatagen create() {
        return new ALStructureDatagen();
    }

    /** 登记一个 grid profile。 */
    public ALStructureDatagen profile(ResourceKey<ALGridProfile> key, ALGridProfile profile) {
        this.profiles.put(key, profile);
        return this;
    }

    /** 登记一个 {@code atlas:jigsaw} 结构（不自动建 set）。 */
    public ALStructureDatagen structure(
            ResourceKey<Structure> key,
            ResourceKey<ALGridProfile> profileKey,
            JigsawFactory factory
    ) {
        this.structures.put(key, new StructureEntry(profileKey, factory));
        return this;
    }

    /** 登记一个 structure_set。 */
    public ALStructureDatagen set(ResourceKey<StructureSet> key, SetFactory factory) {
        this.sets.put(key, factory);
        return this;
    }

    /**
     * <b>推荐用法</b>：登记结构，并自动生成它的 structure_set
     * （同一 profile + {@code atlas:per_chunk} 放置）—— 两者参数一致因此不可能写错。
     */
    public ALStructureDatagen jigsaw(
            ResourceKey<Structure> structureKey,
            ResourceKey<StructureSet> setKey,
            ResourceKey<ALGridProfile> profileKey,
            JigsawFactory factory
    ) {
        return this.structure(structureKey, profileKey, factory)
                .set(setKey, (structures, profiles) -> new StructureSet(
                        structures.getOrThrow(structureKey),
                        new ALGridPlacement(profiles.getOrThrow(profileKey))));
    }

    /** 接进 reginth：内部就是三条 {@code DataProviderInitializer#add}（空集合会跳过）。 */
    public void register(com.altnoir.abysslib.reginth.AbstractReginth<?> reginth) {
        this.register(reginth.getDataGenInitializer());
    }

    /** 接进 reginth 的 provider 初始化器（不想依赖 {@code AbstractReginth} 时的入口）。 */
    public void register(com.altnoir.abysslib.reginth.providers.DataProviderInitializer initializer) {
        if (!this.profiles.isEmpty()) {
            initializer.add(ALGridProfile.KEY, this::bootstrapProfiles);
        }
        if (!this.structures.isEmpty()) {
            initializer.add(Registries.STRUCTURE, this::bootstrapStructures);
        }
        if (!this.sets.isEmpty()) {
            initializer.add(Registries.STRUCTURE_SET, this::bootstrapSets);
        }
    }

    private void bootstrapProfiles(BootstrapContext<ALGridProfile> context) {
        this.profiles.forEach(context::register);
    }

    private void bootstrapStructures(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        HolderGetter<ALGridProfile> profileLookup = context.lookup(ALGridProfile.KEY);
        this.structures.forEach((key, entry) -> context.register(
                key, entry.factory().create(biomes, pools, profileLookup.getOrThrow(entry.profileKey()))));
    }

    private void bootstrapSets(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structureLookup = context.lookup(Registries.STRUCTURE);
        HolderGetter<ALGridProfile> profileLookup = context.lookup(ALGridProfile.KEY);
        this.sets.forEach((key, factory) -> context.register(key, factory.create(structureLookup, profileLookup)));
    }
}
