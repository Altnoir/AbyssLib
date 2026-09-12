package com.altnoir.abysslib.reginth.providers;

import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public interface ReginthLookupFillerProvider extends ReginthProvider {

    CompletableFuture<HolderLookup.Provider> getFilledProvider();

}
