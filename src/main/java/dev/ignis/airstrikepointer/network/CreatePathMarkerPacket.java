package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.client.MarkerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record CreatePathMarkerPacket(
        UUID markerId,
        UUID ownerId,
        Vec3 startPos,
        Vec3 endPos,
        float height,
        int color,
        String teamName,
        int lifetimeTicks,
        boolean isPreview
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(markerId);
        buf.writeUUID(ownerId);
        buf.writeDouble(startPos.x);
        buf.writeDouble(startPos.y);
        buf.writeDouble(startPos.z);
        buf.writeBoolean(endPos != null);
        if (endPos != null) {
            buf.writeDouble(endPos.x);
            buf.writeDouble(endPos.y);
            buf.writeDouble(endPos.z);
        }
        buf.writeFloat(height);
        buf.writeInt(color);
        buf.writeUtf(teamName);
        buf.writeInt(lifetimeTicks);
        buf.writeBoolean(isPreview);
    }

    public static CreatePathMarkerPacket decode(FriendlyByteBuf buf) {
        UUID markerId = buf.readUUID();
        UUID ownerId = buf.readUUID();
        Vec3 startPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 endPos = null;
        if (buf.readBoolean()) {
            endPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
        float height = buf.readFloat();
        int color = buf.readInt();
        String teamName = buf.readUtf();
        int lifetimeTicks = buf.readInt();
        boolean isPreview = buf.readBoolean();
        return new CreatePathMarkerPacket(markerId, ownerId, startPos, endPos, height, color, teamName, lifetimeTicks, isPreview);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MarkerRenderer.addPathMarker(this);
        });
        ctx.get().setPacketHandled(true);
    }
}
