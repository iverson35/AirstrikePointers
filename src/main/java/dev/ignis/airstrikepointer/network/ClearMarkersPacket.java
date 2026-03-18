package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.client.MarkerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record ClearMarkersPacket(UUID ownerId) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(ownerId);
    }

    public static ClearMarkersPacket decode(FriendlyByteBuf buf) {
        return new ClearMarkersPacket(buf.readUUID());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MarkerRenderer.clearMarkersByOwner(ownerId);
        });
        ctx.get().setPacketHandled(true);
    }
}
