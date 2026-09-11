package com.altnoir.abysslib.mixin.model;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.altnoir.abysslib.model.impl.loading.ALModelDefinitions;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public class ModelManagerMixin {

    // Force load our definitions before loading the vanilla ones
    @WrapOperation(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelManager;loadBlockModels(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Map<ResourceLocation, BlockModel>> wrapReload(
            ResourceManager resourceManager,
            Executor executor,
            Operation<CompletableFuture<Map<ResourceLocation, BlockModel>>> original
    ) {
        return CompletableFuture.runAsync(() -> ALModelDefinitions.reload(resourceManager), executor)
                .thenCompose((aVoid) -> original.call(resourceManager, executor));
    }
}