package dev.ignis.airstrikepointer.markers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public abstract class MarkerData {
    protected final UUID markerId;
    protected final UUID ownerId;
    protected final int color;
    protected final String teamName;
    protected int remainingTicks;

    public MarkerData(UUID markerId, UUID ownerId, int color, String teamName, int lifetimeTicks) {
        this.markerId = markerId;
        this.ownerId = ownerId;
        this.color = color;
        this.teamName = teamName;
        this.remainingTicks = lifetimeTicks;
    }

    public UUID getMarkerId() { return markerId; }
    public UUID getOwnerId() { return ownerId; }
    public int getColor() { return color; }
    public String getTeamName() { return teamName; }
    public int getRemainingTicks() { return remainingTicks; }

    public boolean tick() {
        remainingTicks--;
        return remainingTicks <= 0;
    }

    public abstract CompoundTag save();
    public abstract void writeToPacket(CompoundTag tag);
}
