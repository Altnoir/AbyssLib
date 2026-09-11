package com.altnoir.abysslib.mixin.structure;

import com.altnoir.abysslib.structure.ALStructureLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让结构方块的 size/offset 能突破原版 {@code byte} 编码的 ±48 限制。
 *
 * <p>原版（1.21.1）这个包的读写是这样的：
 * <pre>
 * // 读（构造器）
 * this.offset = new BlockPos(Mth.clamp(buffer.readByte(), -48, 48), ...);
 * this.size   = new Vec3i (Mth.clamp(buffer.readByte(),  0, 48), ...);
 * // 写
 * buffer.writeByte(this.size.getX()); ...
 * </pre>
 * 也就是说：<b>无论 UI / NBT 怎么放宽，收包时都会被夹回 48</b>；而且 {@code byte} 正数上限只有 127，
 * 连 128 都表示不了。这是"改了没生效"最隐蔽的一个环节。
 *
 * <p>方案（与 Integrated API 同思路，但补了它缺的健壮性）：
 * <ul>
 *   <li>当数值超出原版可表达范围时，在包尾<b>追加 6 个 int</b>（offset xyz + size xyz）；</li>
 *   <li>读包时若尾部还有 ≥24 字节才读取这 6 个 int，否则保持原版 ±48 语义
 *       —— Integrated API 没做这个判断，原版端发来的包会直接越界读异常；</li>
 *   <li>数值正常（≤48）时不追加，包体与香草<b>逐字节一致</b>，对原版端零影响。</li>
 * </ul>
 * 结果：双端都装 AbyssLib 时获得完整放宽能力；一端是原版时不崩、只是退回 ±48。
 */
@Mixin(value = ServerboundSetStructureBlockPacket.class)
public class ServerboundSetStructureBlockPacketMixin {

    @Shadow
    @Final
    @Mutable
    private BlockPos offset;

    @Shadow
    @Final
    @Mutable
    private Vec3i size;

    /** 是否需要追加高精度载荷：任一分量超出原版 byte 表达范围。 */
    private boolean abysslib$needsWidePayload() {
        final int lim = ALStructureLimits.VANILLA_STRUCTURE_BLOCK_LIMIT;
        return Math.abs(this.offset.getX()) > lim
                || Math.abs(this.offset.getY()) > lim
                || Math.abs(this.offset.getZ()) > lim
                || this.size.getX() > lim
                || this.size.getY() > lim
                || this.size.getZ() > lim;
    }

    @Inject(method = "write", at = @At("TAIL"), require = 0)
    private void abysslib$writeWideValues(FriendlyByteBuf buffer, CallbackInfo ci) {
        if (!abysslib$needsWidePayload()) {
            return;
        }
        buffer.writeInt(this.offset.getX());
        buffer.writeInt(this.offset.getY());
        buffer.writeInt(this.offset.getZ());
        buffer.writeInt(this.size.getX());
        buffer.writeInt(this.size.getY());
        buffer.writeInt(this.size.getZ());
    }

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"), require = 0)
    private void abysslib$readWideValues(FriendlyByteBuf buffer, CallbackInfo ci) {
        if (buffer.readableBytes() < Integer.BYTES * 6) {
            // 原版端发来的包：不追加载荷，保持 ±48 语义（绝不能在这里读越界）
            return;
        }
        final int max = ALStructureLimits.STRUCTURE_BLOCK_MAX_SIZE;
        this.offset = new BlockPos(
                Mth.clamp(buffer.readInt(), -max, max),
                Mth.clamp(buffer.readInt(), -max, max),
                Mth.clamp(buffer.readInt(), -max, max));
        this.size = new Vec3i(
                Mth.clamp(buffer.readInt(), 0, max),
                Mth.clamp(buffer.readInt(), 0, max),
                Mth.clamp(buffer.readInt(), 0, max));
    }
}
