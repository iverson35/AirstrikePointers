package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.markers.MarkerStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端请求清除自己的所有标记
 */
public record RequestClearMarkersPacket() {
    public void encode(FriendlyByteBuf buf) {
        // 无数据需要编码
    }

    public static RequestClearMarkersPacket decode(FriendlyByteBuf buf) {
        return new RequestClearMarkersPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MarkerStorage.get(player.level()).clearMarkersByOwner(player.getUUID());
                player.displayClientMessage(Component.translatable("message.airstrikepointers.markers_cleared").withStyle(ChatFormatting.GREEN), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
