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

// 仅超出原版范围时附加扩展值，普通数据包格式保持不变。
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
            // 原版端只发送 byte 载荷。
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
