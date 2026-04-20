package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.client.MarkerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record UpdateEntityLostPacket(
        UUID markerId,
        boolean lost
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(markerId);
        buf.writeBoolean(lost);
    }

    public static UpdateEntityLostPacket decode(FriendlyByteBuf buf) {
        return new UpdateEntityLostPacket(
                buf.readUUID(),
                buf.readBoolean()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MarkerRenderer.updateEntityLost(this);
        });
        ctx.get().setPacketHandled(true);
    }
}
