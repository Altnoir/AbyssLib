package com.altnoir.abysslib.reginth.providers.loot;

import net.minecraft.core.WritableRegistry;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;

import java.util.Map;

public interface ReginthLootTables extends LootTableSubProvider {
    default void validate(WritableRegistry<LootTable> writableRegistry, ValidationContext validationContext) {}
}
