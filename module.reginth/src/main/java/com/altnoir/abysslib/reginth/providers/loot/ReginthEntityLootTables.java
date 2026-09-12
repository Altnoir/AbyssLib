package com.altnoir.abysslib.reginth.providers.loot;

import com.altnoir.abysslib.reginth.AbstractReginth;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ReginthEntityLootTables extends EntityLootSubProvider implements ReginthLootTables {

    private final AbstractReginth<?> parent;
    private final Consumer<ReginthEntityLootTables> callback;

    private final HolderLookup<Item> itemLookup;
    private final HolderLookup<Block> blockLookup;
    private final HolderLookup<EntityType<?>> entityLookup;

    public ReginthEntityLootTables(HolderLookup.Provider p_346214_, AbstractReginth<?> parent, Consumer<ReginthEntityLootTables> callback) {
        super(FeatureFlags.REGISTRY.allFlags(), p_346214_);
        this.parent = parent;
        this.callback = callback;
        itemLookup = registries.lookupOrThrow(Registries.ITEM);
        blockLookup = registries.lookupOrThrow(Registries.BLOCK);
        entityLookup = registries.lookupOrThrow(Registries.ENTITY_TYPE);
    }

    @Override
    public void generate() {
        callback.accept(this);
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return parent.getAll(Registries.ENTITY_TYPE).stream().map(Supplier::get);
    }

    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }

    public HolderLookup<Item> itemLookup() {
        return itemLookup;
    }

    public HolderLookup<Block> blockLookup() {
        return blockLookup;
    }

    public HolderLookup<EntityType<?>> entityLookup() {
        return entityLookup;
    }

    // @formatter:off
    // GENERATED START - DO NOT EDIT BELOW THIS LINE

    /** Generated override to expose protected method: {@link EntityLootSubProvider#killedByFrog} */
    @Override
    public LootItemCondition.Builder killedByFrog(HolderGetter<EntityType<?>> entityTypes) { return super.killedByFrog(entityTypes); }

    /** Generated override to expose protected method: {@link EntityLootSubProvider#add} */
    @Override
    public void add(EntityType<?> type, LootTable.Builder builder) { super.add(type, builder); }

    /** Generated override to expose protected method: {@link EntityLootSubProvider#add} */
    @Override
    public void add(EntityType<?> type, ResourceKey<LootTable> lootTable, LootTable.Builder builder) { super.add(type, lootTable, builder); }

    // GENERATED END
}
