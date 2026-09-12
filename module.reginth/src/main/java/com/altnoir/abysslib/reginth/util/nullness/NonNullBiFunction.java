package com.altnoir.abysslib.reginth.util.nullness;

import java.util.function.BiFunction;

@FunctionalInterface
public interface NonNullBiFunction<T, U, R> extends BiFunction<T, U, R> {
    
    @Override
    R apply(T t, U u);
}
