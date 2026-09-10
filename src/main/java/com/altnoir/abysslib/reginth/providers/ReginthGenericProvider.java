package com.altnoir.abysslib.reginth.providers;

import com.altnoir.abysslib.reginth.AbstractReginth;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ReginthGenericProvider implements ReginthProvider
{
    private final AbstractReginth<?> Reginth;
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private final ExistingFileHelper existingFileHelper;
    private final LogicalSide side;
    private final ProviderType<ReginthGenericProvider> providerType;
    private final List<Generator> generators = Lists.newArrayList();

    @ApiStatus.Internal
    ReginthGenericProvider(AbstractReginth<?> Reginth, GatherDataEvent event, LogicalSide side, ProviderType<ReginthGenericProvider> providerType)
    {
        this.Reginth = Reginth;
        this.side = side;
        this.providerType = providerType;

        output = event.getGenerator().getPackOutput();
        registries = event.getLookupProvider();
        existingFileHelper = event.getExistingFileHelper();
    }

    public ReginthGenericProvider add(Generator generator)
    {
        generators.add(generator);
        return this;
    }

    @Override
    public LogicalSide getSide()
    {
        return side;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache)
    {
        generators.clear();
        var data = new GeneratorData(output, registries, existingFileHelper);
        Reginth.genData(providerType, this);
        return CompletableFuture.allOf(generators
                .stream()
                .map(generator -> generator.generate(data))
                .map(provider -> provider.run(cache))
                .toArray(CompletableFuture[]::new)
        );
    }

    @Override
    public String getName()
    {
        return "generic_%s_provider".formatted(side.name().toLowerCase(Locale.ROOT));
    }

    public record GeneratorData(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper)
    {
    }

    @FunctionalInterface
    public interface Generator
    {
        DataProvider generate(GeneratorData data);
    }
}
