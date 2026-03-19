package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.client.MarkerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record RemoveMarkerPacket(UUID markerId, boolean isPathMarker) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(markerId);
        buf.writeBoolean(isPathMarker);
    }

    public static RemoveMarkerPacket decode(FriendlyByteBuf buf) {
        return new RemoveMarkerPacket(buf.readUUID(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MarkerRenderer.removeMarker(markerId, isPathMarker);
        });
        ctx.get().setPacketHandled(true);
    }
}
