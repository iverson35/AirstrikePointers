package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.client.MarkerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record SyncMarkersPacket(
        List<PointMarkerData> pointMarkers,
        List<PathMarkerData> pathMarkers
) {
    public record PointMarkerData(
            UUID markerId, UUID ownerId, Vec3 position,
            int color, String teamName, int remainingTicks
    ) {}

    public record PathMarkerData(
            UUID markerId, UUID ownerId, Vec3 startPos, Vec3 endPos,
            float height, int color, String teamName, int remainingTicks
    ) {}

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(pointMarkers.size());
        for (PointMarkerData data : pointMarkers) {
            buf.writeUUID(data.markerId);
            buf.writeUUID(data.ownerId);
            buf.writeDouble(data.position.x);
            buf.writeDouble(data.position.y);
            buf.writeDouble(data.position.z);
            buf.writeInt(data.color);
            buf.writeUtf(data.teamName);
            buf.writeInt(data.remainingTicks);
        }

        buf.writeInt(pathMarkers.size());
        for (PathMarkerData data : pathMarkers) {
            buf.writeUUID(data.markerId);
            buf.writeUUID(data.ownerId);
            buf.writeDouble(data.startPos.x);
            buf.writeDouble(data.startPos.y);
            buf.writeDouble(data.startPos.z);
            buf.writeBoolean(data.endPos != null);
            if (data.endPos != null) {
                buf.writeDouble(data.endPos.x);
                buf.writeDouble(data.endPos.y);
                buf.writeDouble(data.endPos.z);
            }
            buf.writeFloat(data.height);
            buf.writeInt(data.color);
            buf.writeUtf(data.teamName);
            buf.writeInt(data.remainingTicks);
        }
    }

    public static SyncMarkersPacket decode(FriendlyByteBuf buf) {
        int pointCount = buf.readInt();
        List<PointMarkerData> points = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            points.add(new PointMarkerData(
                    buf.readUUID(), buf.readUUID(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readInt(), buf.readUtf(), buf.readInt()
            ));
        }

        int pathCount = buf.readInt();
        List<PathMarkerData> paths = new ArrayList<>(pathCount);
        for (int i = 0; i < pathCount; i++) {
            UUID markerId = buf.readUUID();
            UUID ownerId = buf.readUUID();
            Vec3 startPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 endPos = null;
            if (buf.readBoolean()) {
                endPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            }
            paths.add(new PathMarkerData(
                    markerId, ownerId, startPos, endPos,
                    buf.readFloat(), buf.readInt(), buf.readUtf(), buf.readInt()
            ));
        }

        return new SyncMarkersPacket(points, paths);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MarkerRenderer.clearAllMarkers();
            for (PointMarkerData data : pointMarkers) {
                MarkerRenderer.addPointMarker(new CreatePointMarkerPacket(
                        data.markerId, data.ownerId, data.position,
                        data.color, data.teamName, data.remainingTicks
                ));
            }
            for (PathMarkerData data : pathMarkers) {
                MarkerRenderer.addPathMarker(new CreatePathMarkerPacket(
                        data.markerId, data.ownerId, data.startPos, data.endPos,
                        data.height, data.color, data.teamName, data.remainingTicks, false
                ));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
