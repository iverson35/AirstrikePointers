package dev.ignis.airstrikepointer.markers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class PathMarker extends MarkerData {
    private final Vec3 startPos;
    private Vec3 endPos;
    private final float height;
    private final String itemName; // null if item has no custom name

    public PathMarker(UUID markerId, UUID ownerId, Vec3 startPos, Vec3 endPos, float height, int color, String teamName, int lifetimeTicks) {
        this(markerId, ownerId, startPos, endPos, height, color, teamName, lifetimeTicks, null);
    }

    public PathMarker(UUID markerId, UUID ownerId, Vec3 startPos, Vec3 endPos, float height, int color, String teamName, int lifetimeTicks, String itemName) {
        super(markerId, ownerId, color, teamName, lifetimeTicks);
        this.startPos = startPos;
        this.endPos = endPos;
        this.height = height;
        this.itemName = itemName;
    }

    public Vec3 getStartPos() { return startPos; }
    public Vec3 getEndPos() { return endPos; }
    public float getHeight() { return height; }
    public String getItemName() { return itemName; }
    public boolean isComplete() { return endPos != null; }

    public void setEndPos(Vec3 endPos) {
        this.endPos = endPos;
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("markerId", markerId);
        tag.putUUID("ownerId", ownerId);
        tag.putDouble("startX", startPos.x);
        tag.putDouble("startY", startPos.y);
        tag.putDouble("startZ", startPos.z);
        tag.putBoolean("hasEnd", endPos != null);
        if (endPos != null) {
            tag.putDouble("endX", endPos.x);
            tag.putDouble("endY", endPos.y);
            tag.putDouble("endZ", endPos.z);
        }
        tag.putFloat("height", height);
        tag.putInt("color", color);
        tag.putString("teamName", teamName);
        tag.putInt("remainingTicks", remainingTicks);
        if (itemName != null) {
            tag.putString("itemName", itemName);
        }
        return tag;
    }

    @Override
    public void writeToPacket(CompoundTag tag) {
        tag.putUUID("markerId", markerId);
        tag.putUUID("ownerId", ownerId);
        tag.putDouble("startX", startPos.x);
        tag.putDouble("startY", startPos.y);
        tag.putDouble("startZ", startPos.z);
        tag.putBoolean("hasEnd", endPos != null);
        if (endPos != null) {
            tag.putDouble("endX", endPos.x);
            tag.putDouble("endY", endPos.y);
            tag.putDouble("endZ", endPos.z);
        }
        tag.putFloat("height", height);
        tag.putInt("color", color);
        tag.putString("teamName", teamName);
        tag.putInt("remainingTicks", remainingTicks);
        if (itemName != null) {
            tag.putString("itemName", itemName);
        }
    }

    public static PathMarker load(CompoundTag tag) {
        Vec3 startPos = new Vec3(tag.getDouble("startX"), tag.getDouble("startY"), tag.getDouble("startZ"));
        Vec3 endPos = null;
        if (tag.getBoolean("hasEnd")) {
            endPos = new Vec3(tag.getDouble("endX"), tag.getDouble("endY"), tag.getDouble("endZ"));
        }
        String itemName = tag.contains("itemName") ? tag.getString("itemName") : null;
        return new PathMarker(
                tag.getUUID("markerId"),
                tag.getUUID("ownerId"),
                startPos,
                endPos,
                tag.getFloat("height"),
                tag.getInt("color"),
                tag.getString("teamName"),
                tag.getInt("remainingTicks"),
                itemName
        );
    }
}
