package com.altnoir.abysslib.model.impl.client.models;

import com.altnoir.abysslib.model.api.client.models.ALBlockModel;
import com.altnoir.abysslib.model.api.client.models.ALModelAttributes;
import com.altnoir.abysslib.model.api.client.models.ALQuad;
import com.altnoir.abysslib.model.api.client.utils.AppearanceAndTintGetter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 属性装饰器：把 JSON 里声明的属性（{@code tint} / {@code render_type} / 本库扩展的
 * {@code abysslib:emissive}）套到任意内置模型类型上，其余行为全部转发给被装饰的模型。
 * <p>
 * 上游只有 {@code athena:ctm}（{@link ConnectedBlockModel}）自己解析属性，其余类型一律返回
 * {@link ALModelAttributes#EMPTY}；本库在 {@code DefaultModels} 注册内置类型时统一包装，
 * 这样发光等属性对所有类型（carpet/pane/giant/pillar/…）都生效，同时不改动上游各模型类本体，
 * 便于日后与上游 diff。
 */
public record AttributeOverrideModel(ALBlockModel delegate, ALModelAttributes attributes) implements ALBlockModel {

    @Override
    public List<ALQuad> getQuads(AppearanceAndTintGetter level, BlockState state, BlockPos pos, @Nullable Direction direction) {
        return delegate.getQuads(level, state, pos, direction);
    }

    @Override
    public Map<Direction, List<ALQuad>> getDefaultQuads(@Nullable Direction direction) {
        return delegate.getDefaultQuads(direction);
    }

    @Override
    public Int2ObjectMap<TextureAtlasSprite> getTextures(Function<Material, TextureAtlasSprite> getter) {
        return delegate.getTextures(getter);
    }

    @Override
    public ALModelAttributes getAttributes() {
        return attributes;
    }
}
