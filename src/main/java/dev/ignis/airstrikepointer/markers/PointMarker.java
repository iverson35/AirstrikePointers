package dev.ignis.airstrikepointer.markers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class PointMarker extends MarkerData {
    private final Vec3 position;

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks) {
        super(markerId, ownerId, color, teamName, lifetimeTicks);
        this.position = position;
    }

    public Vec3 getPosition() { return position; }

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
    }

    public static PointMarker load(CompoundTag tag) {
        return new PointMarker(
                tag.getUUID("markerId"),
                tag.getUUID("ownerId"),
                new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")),
                tag.getInt("color"),
                tag.getString("teamName"),
                tag.getInt("remainingTicks")
        );
    }
}
