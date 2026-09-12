package com.altnoir.abysslib.reginth.providers;

import net.minecraft.data.DataProvider;
import net.minecraft.resources.Identifier;

public interface ReginthProviderDelegate<R, T extends R> extends DataProvider {
    
    String getName();
    
    Identifier getId();
    
    T getEntry();
}
