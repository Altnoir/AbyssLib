package com.altnoir.abysslib.reginth.providers;

import net.minecraft.data.DataProvider;
import net.neoforged.fml.LogicalSide;

public interface ReginthProvider extends DataProvider {
    
    LogicalSide getSide();
}
