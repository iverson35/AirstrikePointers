package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.client.MarkerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record UpdatePointMarkerPacket(
        UUID markerId,
        Vec3 position
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(markerId);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
    }

    public static UpdatePointMarkerPacket decode(FriendlyByteBuf buf) {
        return new UpdatePointMarkerPacket(
                buf.readUUID(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MarkerRenderer.updatePointMarkerPosition(this);
        });
        ctx.get().setPacketHandled(true);
    }
}
