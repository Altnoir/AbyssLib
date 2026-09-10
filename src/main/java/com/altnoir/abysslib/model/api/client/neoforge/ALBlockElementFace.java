package com.altnoir.abysslib.model.api.client.neoforge;

import com.altnoir.abysslib.model.api.client.models.ALModelAttributes;
import com.altnoir.abysslib.model.api.client.models.ALQuad;
import com.altnoir.abysslib.model.api.client.models.TintProvider;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3f;

@ApiStatus.Internal
public class ALBlockElementFace {

    /** 发光（{@link ALModelAttributes#EMISSIVE_KEY}）时写入面的光照等级：15/15 = 全亮。 */
    public static final int EMISSIVE_LIGHT = 15;

    /**
     * 构造面的烘焙数据。
     * <p>
     * 光照说明（NeoForge 语义）：{@code FaceBakery} 只有在 {@code ExtraFaceData != DEFAULT} 时才会把它写进
     * quad 顶点，且顶点光照是被渲染端 {@code max(烘焙光照, 世界光照)} 合并的，所以写入 0 等于"不干预光照"
     * （这正是上游静态 tint 分支传 {@code DEFAULT.blockLight()/skyLight()} 的实际效果），
     * 而写入 15 则任何环境下都是全亮 = 发光。
     */
    public static BlockElementFace of(ALQuad quad, Direction direction, Vector3f start, Vector3f end, ALModelAttributes attributes) {
        int tintIndex = -1;
        ExtraFaceData extraData = null;
        final TintProvider tint = attributes == null ? null : attributes.getTint();
        final boolean emissive = attributes != null && attributes.isEmissive();
        // 非发光时沿用上游写法（DEFAULT 的 0/0 与 AO=true）；发光时 15/15 + 关 AO
        final int light = emissive ? EMISSIVE_LIGHT : ExtraFaceData.DEFAULT.blockLight();
        final int skyLight = emissive ? EMISSIVE_LIGHT : ExtraFaceData.DEFAULT.skyLight();
        final boolean ao = !emissive && ExtraFaceData.DEFAULT.ambientOcclusion();

        switch (tint) {
            case TintProvider.Index(var index) -> {
                tintIndex = index;
                if (emissive) {
                    // 需要发光但颜色走 tintindex（由 BlockColor 提供）时，只为光照写一份数据
                    extraData = new ExtraFaceData(ExtraFaceData.DEFAULT.color(), light, skyLight, ao);
                }
            }
            case TintProvider.Static(var color) -> extraData = new ExtraFaceData(color, light, skyLight, ao);
            case null -> {
                if (emissive) {
                    extraData = new ExtraFaceData(ExtraFaceData.DEFAULT.color(), light, skyLight, ao);
                }
            }
        }

        return new BlockElementFace(
                quad.cull() ? direction : null,
                tintIndex,
                "",
                new BlockFaceUV(getUVs(start, end, direction), (int)((quad.rotation().ordinal() * 90f) % 360f)),
                extraData,
                new MutableObject<>()
        );
    }

    private static float[] getUVs(Vector3f from, Vector3f to, Direction direction) {
        float[] uvs = switch (direction) {
            case UP -> new float[] { from.x(), to.z(), to.x(), from.z() };
            case DOWN -> new float[]{ from.x(), 16 - from.z(), to.x(), 16 - to.z() };
            case NORTH -> new float[] { 16 - from.x, 16 - to.y, 16 - to.x, 16 - from.y };
            case SOUTH -> new float[]{ to.x, 16 - to.y, from.x, 16 - from.y };
            case WEST -> new float[]{ to.z(), 16.0F - to.y(), from.z(), 16.0F - from.y() };
            case EAST -> new float[]{16.0F - from.z, 16.0F - to.y(), 16.0F - to.z, 16.0F - from.y()};
        };
        return new float[] { uvs[0], uvs[1], uvs[2], uvs[3] };
    }
}
