package com.altnoir.abysslib.reginth.providers.loot;

import com.altnoir.abysslib.reginth.AbstractReginth;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ReginthBlockLootTables extends BlockLootSubProvider implements ReginthLootTables {
    private final AbstractReginth<?> parent;
    private final Consumer<ReginthBlockLootTables> callback;

    private final HolderLookup<Item> itemLookup;
    private final HolderLookup<Block> blockLookup;
    private final HolderLookup<EntityType<?>> entityLookup;

    public ReginthBlockLootTables(HolderLookup.Provider provider, AbstractReginth<?> parent, Consumer<ReginthBlockLootTables> callback) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        this.parent = parent;
        this.callback = callback;
        itemLookup = registries.lookupOrThrow(Registries.ITEM);
        blockLookup = registries.lookupOrThrow(Registries.BLOCK);
        entityLookup = registries.lookupOrThrow(Registries.ENTITY_TYPE);
    }

    @Override
    protected void generate() {
        callback.accept(this);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return parent.getAll(Registries.BLOCK).stream().map(Supplier::get).collect(Collectors.toList());
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

    /** Generated override to expose protected method: {@link BlockLootSubProvider#applyExplosionDecay} */
    @Override
    public <T extends FunctionUserBuilder<T>> T applyExplosionDecay(ItemLike type, FunctionUserBuilder<T> builder) { return super.applyExplosionDecay(type, builder); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#applyExplosionCondition} */
    @Override
    public <T extends ConditionUserBuilder<T>> T applyExplosionCondition(ItemLike type, ConditionUserBuilder<T> builder) { return super.applyExplosionCondition(type, builder); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createSelfDropDispatchTable} */
    public static LootTable.Builder createSelfDropDispatchTable(Block original, LootItemCondition.Builder condition, LootPoolEntryContainer.Builder<?> entry) { return BlockLootSubProvider.createSelfDropDispatchTable(original, condition, entry); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createSilkTouchDispatchTable} */
    @Override
    public LootTable.Builder createSilkTouchDispatchTable(Block original, LootPoolEntryContainer.Builder<?> entry) { return super.createSilkTouchDispatchTable(original, entry); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createShearsDispatchTable} */
    @Override
    public LootTable.Builder createShearsDispatchTable(Block original, LootPoolEntryContainer.Builder<?> entry) { return super.createShearsDispatchTable(original, entry); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createSilkTouchOrShearsDispatchTable} */
    @Override
    public LootTable.Builder createSilkTouchOrShearsDispatchTable(Block original, LootPoolEntryContainer.Builder<?> entry) { return super.createSilkTouchOrShearsDispatchTable(original, entry); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createSingleItemTableWithSilkTouch} */
    @Override
    public LootTable.Builder createSingleItemTableWithSilkTouch(Block original, ItemLike drop) { return super.createSingleItemTableWithSilkTouch(original, drop); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createSingleItemTable} */
    @Override
    public LootTable.Builder createSingleItemTable(ItemLike drop, NumberProvider count) { return super.createSingleItemTable(drop, count); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createSingleItemTableWithSilkTouch} */
    @Override
    public LootTable.Builder createSingleItemTableWithSilkTouch(Block original, ItemLike drop, NumberProvider count) { return super.createSingleItemTableWithSilkTouch(original, drop, count); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createSilkTouchOnlyTable} */
    @Override
    public LootTable.Builder createSilkTouchOnlyTable(ItemLike drop) { return super.createSilkTouchOnlyTable(drop); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createPotFlowerItemTable} */
    @Override
    public LootTable.Builder createPotFlowerItemTable(ItemLike flower) { return super.createPotFlowerItemTable(flower); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createSlabItemTable} */
    @Override
    public LootTable.Builder createSlabItemTable(Block slab) { return super.createSlabItemTable(slab); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createNameableBlockEntityTable} */
    @Override
    public LootTable.Builder createNameableBlockEntityTable(Block drop) { return super.createNameableBlockEntityTable(drop); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createShulkerBoxDrop} */
    @Override
    public LootTable.Builder createShulkerBoxDrop(Block shulkerBox) { return super.createShulkerBoxDrop(shulkerBox); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createCopperOreDrops} */
    @Override
    public LootTable.Builder createCopperOreDrops(Block block) { return super.createCopperOreDrops(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createLapisOreDrops} */
    @Override
    public LootTable.Builder createLapisOreDrops(Block block) { return super.createLapisOreDrops(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createRedstoneOreDrops} */
    @Override
    public LootTable.Builder createRedstoneOreDrops(Block block) { return super.createRedstoneOreDrops(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createBannerDrop} */
    @Override
    public LootTable.Builder createBannerDrop(Block original) { return super.createBannerDrop(original); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createBeeNestDrop} */
    @Override
    public LootTable.Builder createBeeNestDrop(Block original) { return super.createBeeNestDrop(original); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createBeeHiveDrop} */
    @Override
    public LootTable.Builder createBeeHiveDrop(Block original) { return super.createBeeHiveDrop(original); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createCaveVinesDrop} */
    @Override
    public LootTable.Builder createCaveVinesDrop(Block original) { return super.createCaveVinesDrop(original); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createCopperGolemStatueBlock} */
    @Override
    public LootTable.Builder createCopperGolemStatueBlock(Block block) { return super.createCopperGolemStatueBlock(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createOreDrop} */
    @Override
    public LootTable.Builder createOreDrop(Block original, Item drop) { return super.createOreDrop(original, drop); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createMushroomBlockDrop} */
    @Override
    public LootTable.Builder createMushroomBlockDrop(Block original, ItemLike drop) { return super.createMushroomBlockDrop(original, drop); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createGrassDrops} */
    @Override
    public LootTable.Builder createGrassDrops(Block original) { return super.createGrassDrops(original); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createShearsOnlyDrop} */
    @Override
    public LootTable.Builder createShearsOnlyDrop(ItemLike drop) { return super.createShearsOnlyDrop(drop); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createShearsOrSilkTouchOnlyDrop} */
    @Override
    public LootTable.Builder createShearsOrSilkTouchOnlyDrop(ItemLike drop) { return super.createShearsOrSilkTouchOnlyDrop(drop); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createMultifaceBlockDrops} */
    @Override
    public LootTable.Builder createMultifaceBlockDrops(Block block, LootItemCondition.Builder condition) { return super.createMultifaceBlockDrops(block, condition); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createMultifaceBlockDrops} */
    @Override
    public LootTable.Builder createMultifaceBlockDrops(Block block) { return super.createMultifaceBlockDrops(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createMossyCarpetBlockDrops} */
    @Override
    public LootTable.Builder createMossyCarpetBlockDrops(Block block) { return super.createMossyCarpetBlockDrops(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createLeavesDrops} */
    @Override
    public LootTable.Builder createLeavesDrops(Block original, Block sapling, float... saplingChances) { return super.createLeavesDrops(original, sapling, saplingChances); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createOakLeavesDrops} */
    @Override
    public LootTable.Builder createOakLeavesDrops(Block original, Block sapling, float... saplingChances) { return super.createOakLeavesDrops(original, sapling, saplingChances); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createMangroveLeavesDrops} */
    @Override
    public LootTable.Builder createMangroveLeavesDrops(Block block) { return super.createMangroveLeavesDrops(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createCropDrops} */
    @Override
    public LootTable.Builder createCropDrops(Block original, Item cropDrop, Item seedDrop, LootItemCondition.Builder isMaxAge) { return super.createCropDrops(original, cropDrop, seedDrop, isMaxAge); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createDoublePlantShearsDrop} */
    @Override
    public LootTable.Builder createDoublePlantShearsDrop(Block block) { return super.createDoublePlantShearsDrop(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createDoublePlantWithSeedDrops} */
    @Override
    public LootTable.Builder createDoublePlantWithSeedDrops(Block block, Block drop) { return super.createDoublePlantWithSeedDrops(block, drop); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createCandleDrops} */
    @Override
    public LootTable.Builder createCandleDrops(Block block) { return super.createCandleDrops(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createCandleCakeDrops} */
    public static LootTable.Builder createCandleCakeDrops(Block candle) { return BlockLootSubProvider.createCandleCakeDrops(candle); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#addNetherVinesDropTable} */
    @Override
    public void addNetherVinesDropTable(Block vineBlock, Block plantBlock) { super.addNetherVinesDropTable(vineBlock, plantBlock); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#createDoorTable} */
    @Override
    public LootTable.Builder createDoorTable(Block block) { return super.createDoorTable(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#dropPottedContents} */
    @Override
    public void dropPottedContents(Block potted) { super.dropPottedContents(potted); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#otherWhenSilkTouch} */
    @Override
    public void otherWhenSilkTouch(Block block, Block other) { super.otherWhenSilkTouch(block, other); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#dropOther} */
    @Override
    public void dropOther(Block block, ItemLike drop) { super.dropOther(block, drop); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#dropWhenSilkTouch} */
    @Override
    public void dropWhenSilkTouch(Block block) { super.dropWhenSilkTouch(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#dropSelf} */
    @Override
    public void dropSelf(Block block) { super.dropSelf(block); }

    /** Generated override to expose protected method: {@link BlockLootSubProvider#add} */
    @Override
    public void add(Block block, LootTable.Builder builder) { super.add(block, builder); }

    // GENERATED END
}
