package com.smogline.network.packet;

import com.smogline.block.entity.custom.MotorElectroBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketToggleMotorMode {
    private final BlockPos pos;
    public PacketToggleMotorMode(BlockPos pos) { this.pos = pos; }
    public PacketToggleMotorMode(FriendlyByteBuf buf) { this.pos = buf.readBlockPos(); }
    public void encode(FriendlyByteBuf buf) { buf.writeBlockPos(pos); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().getBlockEntity(pos) instanceof MotorElectroBlockEntity be) {
                be.toggleGeneratorMode();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
