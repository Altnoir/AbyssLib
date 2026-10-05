package com.altnoir.abysslib.structure;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.LinkedHashMap;
import java.util.Map;

// 汇总数据生成所需的 profile、jigsaw 结构和 structure set。
public final class ALStructureDatagen {

    @FunctionalInterface
    public interface JigsawFactory {
        ALJigsawStructure create(
                HolderGetter<Biome> biomes,
                HolderGetter<StructureTemplatePool> pools,
                Holder<ALGridProfile> profile);
    }

    @FunctionalInterface
    public interface SetFactory {
        StructureSet create(HolderGetter<Structure> structures, HolderGetter<ALGridProfile> profiles);
    }

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

    public ALStructureDatagen profile(ResourceKey<ALGridProfile> key, ALGridProfile profile) {
        this.profiles.put(key, profile);
        return this;
    }

    public ALStructureDatagen structure(
            ResourceKey<Structure> key,
            ResourceKey<ALGridProfile> profileKey,
            JigsawFactory factory
    ) {
        this.structures.put(key, new StructureEntry(profileKey, factory));
        return this;
    }

    public ALStructureDatagen set(ResourceKey<StructureSet> key, SetFactory factory) {
        this.sets.put(key, factory);
        return this;
    }

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

    public void register(com.altnoir.abysslib.reginth.AbstractReginth<?> reginth) {
        this.register(reginth.getDataGenInitializer());
    }

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
