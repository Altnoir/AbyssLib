package com.altnoir.abysslib.reginth.providers.loot;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;


import com.altnoir.abysslib.reginth.AbstractReginth;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.packs.VanillaEntityLoot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ReginthEntityLootTables extends VanillaEntityLoot implements ReginthLootTables {

    private final AbstractReginth<?> parent;
    private final Consumer<ReginthEntityLootTables> callback;

    public ReginthEntityLootTables(HolderLookup.Provider p_346214_, AbstractReginth<?> parent, Consumer<ReginthEntityLootTables> callback) {
        super(p_346214_);
        this.parent = parent;
        this.callback = callback;
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

    // @formatter:off
    // GENERATED START - DO NOT EDIT BELOW THIS LINE

    /** Generated override to expose protected method: {@link EntityLootSubProvider#createSheepTable} */
    public static LootTable.Builder createSheepTable(ItemLike p_249422_) { return EntityLootSubProvider.createSheepTable(p_249422_); }

    /** Generated override to expose protected method: {@link EntityLootSubProvider#canHaveLootTable} */
    @Override
    public boolean canHaveLootTable(EntityType<?> p_249029_) { return super.canHaveLootTable(p_249029_); }

    /** Generated override to expose protected method: {@link EntityLootSubProvider#killedByFrogVariant} */
    @Override
    public LootItemCondition.Builder killedByFrogVariant(ResourceKey<FrogVariant> p_335676_) { return super.killedByFrogVariant(p_335676_); }

    /** Generated override to expose protected method: {@link EntityLootSubProvider#add} */
    @Override
    public void add(EntityType<?> p_248740_, LootTable.Builder p_249440_) { super.add(p_248740_, p_249440_); }

    /** Generated override to expose protected method: {@link EntityLootSubProvider#add} */
    @Override
    public void add(EntityType<?> p_252130_, ResourceKey<LootTable> p_335943_, LootTable.Builder p_249357_) { super.add(p_252130_, p_335943_, p_249357_); }

    // GENERATED END
}
