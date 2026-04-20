package dev.ignis.airstrikepointer.markers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class PointMarker extends MarkerData {
    private Vec3 position;
    private final UUID targetEntityId; // null if not tracking an entity
    private final String itemName; // null if item has no custom name

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks) {
        this(markerId, ownerId, position, color, teamName, lifetimeTicks, null, null);
    }

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks, UUID targetEntityId) {
        this(markerId, ownerId, position, color, teamName, lifetimeTicks, targetEntityId, null);
    }

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks, UUID targetEntityId, String itemName) {
        super(markerId, ownerId, color, teamName, lifetimeTicks);
        this.position = position;
        this.targetEntityId = targetEntityId;
        this.itemName = itemName;
    }

    public Vec3 getPosition() { return position; }
    public void setPosition(Vec3 position) { this.position = position; }
    public UUID getTargetEntityId() { return targetEntityId; }
    public String getItemName() { return itemName; }

    public boolean isTrackingEntity() { return targetEntityId != null; }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("markerId", markerId);
        tag.putUUID("ownerId", ownerId);
        tag.putDouble("x", position.x);
        tag.putDouble("y", position.y);
        tag.putDouble("z", position.z);
        tag.putInt("color", color);
        tag.putString("teamName", teamName);
        tag.putInt("remainingTicks", remainingTicks);
        if (targetEntityId != null) {
            tag.putUUID("targetEntityId", targetEntityId);
        }
        if (itemName != null) {
            tag.putString("itemName", itemName);
        }
        return tag;
    }

    @Override
    public void writeToPacket(CompoundTag tag) {
        tag.putUUID("markerId", markerId);
        tag.putUUID("ownerId", ownerId);
        tag.putDouble("x", position.x);
        tag.putDouble("y", position.y);
        tag.putDouble("z", position.z);
        tag.putInt("color", color);
        tag.putString("teamName", teamName);
        tag.putInt("remainingTicks", remainingTicks);
        if (targetEntityId != null) {
            tag.putUUID("targetEntityId", targetEntityId);
        }
        if (itemName != null) {
            tag.putString("itemName", itemName);
        }
    }

    public static PointMarker load(CompoundTag tag) {
        UUID targetEntityId = tag.hasUUID("targetEntityId") ? tag.getUUID("targetEntityId") : null;
        String itemName = tag.contains("itemName") ? tag.getString("itemName") : null;
        return new PointMarker(
                tag.getUUID("markerId"),
                tag.getUUID("ownerId"),
                new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")),
                tag.getInt("color"),
                tag.getString("teamName"),
                tag.getInt("remainingTicks"),
                targetEntityId,
                itemName
        );
    }
}
