package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.client.MarkerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record CreatePointMarkerPacket(
        UUID markerId,
        UUID ownerId,
        Vec3 position,
        int color,
        String teamName,
        int lifetimeTicks
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(markerId);
        buf.writeUUID(ownerId);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeInt(color);
        buf.writeUtf(teamName);
        buf.writeInt(lifetimeTicks);
    }

    public static CreatePointMarkerPacket decode(FriendlyByteBuf buf) {
        return new CreatePointMarkerPacket(
                buf.readUUID(),
                buf.readUUID(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readInt(),
                buf.readUtf(),
                buf.readInt()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MarkerRenderer.addPointMarker(this);
        });
        ctx.get().setPacketHandled(true);
    }
}
