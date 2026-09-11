package com.altnoir.abysslib.reginth.util;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public class ReginthDistExecutor {
    public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> toRun) {
        if (dist == FMLEnvironment.dist) {
            toRun.get().run();
        }
    }
}
