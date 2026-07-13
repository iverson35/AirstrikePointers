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
        int lifetimeTicks,
        int targetType,
        String entityName,
        UUID targetEntityId,
        String itemName,
        String customTitle,
        String customDescription
) {
    public static final int TARGET_MISS = 0;
    public static final int TARGET_BLOCK = 1;
    public static final int TARGET_ENTITY = 2;

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(markerId);
        buf.writeUUID(ownerId);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeInt(color);
        buf.writeUtf(teamName);
        buf.writeInt(lifetimeTicks);
        buf.writeInt(targetType);
        buf.writeUtf(entityName != null ? entityName : "");
        buf.writeBoolean(targetEntityId != null);
        if (targetEntityId != null) {
            buf.writeUUID(targetEntityId);
        }
        buf.writeUtf(itemName != null ? itemName : "");
        buf.writeUtf(customTitle != null ? customTitle : "");
        buf.writeUtf(customDescription != null ? customDescription : "");
    }

    public static CreatePointMarkerPacket decode(FriendlyByteBuf buf) {
        UUID markerId = buf.readUUID();
        UUID ownerId = buf.readUUID();
        Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int color = buf.readInt();
        String teamName = buf.readUtf();
        int lifetimeTicks = buf.readInt();
        int targetType = buf.readInt();
        String entityName = buf.readUtf();
        UUID targetEntityId = buf.readBoolean() ? buf.readUUID() : null;
        String itemName = buf.readUtf();
        if (itemName.isEmpty()) itemName = null;
        String customTitle = buf.readUtf();
        if (customTitle.isEmpty()) customTitle = null;
        String customDescription = buf.readUtf();
        if (customDescription.isEmpty()) customDescription = null;
        return new CreatePointMarkerPacket(markerId, ownerId, position, color, teamName, lifetimeTicks, targetType, entityName, targetEntityId, itemName, customTitle, customDescription);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MarkerRenderer.addPointMarker(this);
        });
        ctx.get().setPacketHandled(true);
    }
}
