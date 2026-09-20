package com.altnoir.abysslib.reginth.providers;

import com.altnoir.abysslib.reginth.AbstractReginth;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.DataMapProvider;

import java.util.concurrent.CompletableFuture;

public class ReginthDataMapProvider extends DataMapProvider implements ReginthProvider {

    private final AbstractReginth<?> parent;

    protected ReginthDataMapProvider(AbstractReginth<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> pvd) {
        super(output, pvd);
        this.parent = parent;
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }

    /**
     * 生成 data map 条目。
     * <p>
     * 重写的是带 {@link HolderLookup.Provider} 参数的重载：NeoForge 21.1 的无参
     * {@code DataMapProvider#gather()} 已经标记为 {@code @Deprecated(forRemoval = true)}，
     * 且 {@code DataMapProvider#run(CachedOutput)} 内部走的就是这个带参重载
     * （无参那个只是默认实现转发过去）。所以这里必须重写带参版本，否则既吃 deprecation
     * 警告，将来 NeoForge 真删掉无参方法时还会静默失效。
     */
    @Override
    protected void gather(HolderLookup.Provider provider) {
        parent.genData(ProviderType.DATA_MAP, this);
    }

}
