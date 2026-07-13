package dev.ignis.airstrikepointer.markers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class PointMarker extends MarkerData {
    private Vec3 position;
    private final UUID targetEntityId;
    private final String iconId;
    private final String itemName;
    private final String entityName;
    private final String customTitle;
    private final String customDescription;
    private boolean entityLost;
    private boolean guidanceDisabled;

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks) {
        this(markerId, ownerId, position, color, teamName, lifetimeTicks, null, null, null, null, null, null, false);
    }

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks, UUID targetEntityId) {
        this(markerId, ownerId, position, color, teamName, lifetimeTicks, targetEntityId, null, null, null, null, null, false);
    }

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks, UUID targetEntityId, String itemName) {
        this(markerId, ownerId, position, color, teamName, lifetimeTicks, targetEntityId, null, itemName, null, null, null, false);
    }

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks, UUID targetEntityId, String itemName, String entityName) {
        this(markerId, ownerId, position, color, teamName, lifetimeTicks, targetEntityId, null, itemName, entityName, null, null, false);
    }

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks, UUID targetEntityId, String iconId, String itemName, String entityName, String customTitle, String customDescription) {
        this(markerId, ownerId, position, color, teamName, lifetimeTicks, targetEntityId, iconId, itemName, entityName, customTitle, customDescription, false);
    }

    public PointMarker(UUID markerId, UUID ownerId, Vec3 position, int color, String teamName, int lifetimeTicks, UUID targetEntityId, String iconId, String itemName, String entityName, String customTitle, String customDescription, boolean guidanceDisabled) {
        super(markerId, ownerId, color, teamName, lifetimeTicks);
        this.position = position;
        this.targetEntityId = targetEntityId;
        this.iconId = iconId != null ? iconId : PointMarkerIcon.getDefaultIconId(targetEntityId != null);
        this.itemName = itemName;
        this.entityName = entityName;
        this.customTitle = customTitle;
        this.customDescription = customDescription;
        this.entityLost = false;
        this.guidanceDisabled = guidanceDisabled;
    }

    public Vec3 getPosition() { return position; }
    public void setPosition(Vec3 position) { this.position = position; }
    public UUID getTargetEntityId() { return targetEntityId; }
    public String getIconId() { return iconId; }
    public String getItemName() { return itemName; }
    public String getEntityName() { return entityName; }
    public String getCustomTitle() { return customTitle; }
    public String getCustomDescription() { return customDescription; }
    public boolean isEntityLost() { return entityLost; }
    public void setEntityLost(boolean lost) { this.entityLost = lost; }
    public boolean isGuidanceDisabled() { return guidanceDisabled; }
    public void setGuidanceDisabled(boolean disabled) { this.guidanceDisabled = disabled; }

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
        if (entityName != null) {
            tag.putString("entityName", entityName);
        }
        if (customTitle != null) {
            tag.putString("customTitle", customTitle);
        }
        if (customDescription != null) {
            tag.putString("customDescription", customDescription);
        }
        tag.putString("iconId", iconId);
        tag.putBoolean("guidanceDisabled", guidanceDisabled);
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
        if (entityName != null) {
            tag.putString("entityName", entityName);
        }
        if (customTitle != null) {
            tag.putString("customTitle", customTitle);
        }
        if (customDescription != null) {
            tag.putString("customDescription", customDescription);
        }
        tag.putString("iconId", iconId);
        tag.putBoolean("guidanceDisabled", guidanceDisabled);
    }

    public static PointMarker load(CompoundTag tag) {
        UUID targetEntityId = tag.hasUUID("targetEntityId") ? tag.getUUID("targetEntityId") : null;
        String itemName = tag.contains("itemName") ? tag.getString("itemName") : null;
        String entityName = tag.contains("entityName") ? tag.getString("entityName") : null;
        String customTitle = tag.contains("customTitle") ? tag.getString("customTitle") : null;
        String customDescription = tag.contains("customDescription") ? tag.getString("customDescription") : null;
        // 向后兼容：旧数据没有 iconId 时根据 targetEntityId 推断
        String iconId = tag.contains("iconId") ? tag.getString("iconId") : null;
        // 向后兼容：旧数据没有 guidanceDisabled 时默认为 false
        boolean guidanceDisabled = tag.contains("guidanceDisabled") && tag.getBoolean("guidanceDisabled");
        return new PointMarker(
                tag.getUUID("markerId"),
                tag.getUUID("ownerId"),
                new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")),
                tag.getInt("color"),
                tag.getString("teamName"),
                tag.getInt("remainingTicks"),
                targetEntityId,
                iconId,
                itemName,
                entityName,
                customTitle,
                customDescription,
                guidanceDisabled
        );
    }
}
