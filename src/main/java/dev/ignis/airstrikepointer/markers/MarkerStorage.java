package dev.ignis.airstrikepointer.markers;

import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.Config;
import dev.ignis.airstrikepointer.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.util.*;

public class MarkerStorage extends SavedData {
    private static final String DATA_NAME = AirstrikePointers.MODID + "_markers";
    private final Map<UUID, PointMarker> pointMarkers = new HashMap<>();
    private final Map<UUID, PathMarker> pathMarkers = new HashMap<>();
    private final Map<UUID, Integer> playerMarkerCount = new HashMap<>();

    public static MarkerStorage get(Level level) {
        if (level.isClientSide) {
            throw new RuntimeException("Cannot access MarkerStorage on client side");
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            server = ServerLifecycleHooks.getCurrentServer();
        }
        if (server == null) {
            throw new RuntimeException("Cannot get server instance");
        }
        return server.overworld().getDataStorage().computeIfAbsent(
                MarkerStorage::load, MarkerStorage::new, DATA_NAME);
    }

    @Nonnull
    @Override
    public CompoundTag save(@Nonnull CompoundTag compound) {
        ListTag pointList = new ListTag();
        for (PointMarker marker : pointMarkers.values()) {
            pointList.add(marker.save());
        }
        compound.put("pointMarkers", pointList);

        ListTag pathList = new ListTag();
        for (PathMarker marker : pathMarkers.values()) {
            pathList.add(marker.save());
        }
        compound.put("pathMarkers", pathList);

        return compound;
    }

    public static MarkerStorage load(CompoundTag compound) {
        MarkerStorage storage = new MarkerStorage();

        ListTag pointList = compound.getList("pointMarkers", 10);
        for (int i = 0; i < pointList.size(); i++) {
            PointMarker marker = PointMarker.load(pointList.getCompound(i));
            storage.pointMarkers.put(marker.getMarkerId(), marker);
            storage.incrementPlayerCount(marker.getOwnerId());
        }

        ListTag pathList = compound.getList("pathMarkers", 10);
        for (int i = 0; i < pathList.size(); i++) {
            PathMarker marker = PathMarker.load(pathList.getCompound(i));
            storage.pathMarkers.put(marker.getMarkerId(), marker);
            storage.incrementPlayerCount(marker.getOwnerId());
        }

        return storage;
    }

    public PointMarker createPointMarker(UUID ownerId, Vec3 position, int color, String teamName, int targetType, String entityName, String playerName) {
        if (getPlayerMarkerCount(ownerId) >= Config.MAX_MARKERS_PER_PLAYER.get()) {
            return null;
        }

        UUID markerId = UUID.randomUUID();
        int lifetimeTicks = Config.MARKER_LIFETIME_SECONDS.get() * 20;
        PointMarker marker = new PointMarker(markerId, ownerId, position, color, teamName, lifetimeTicks);
        pointMarkers.put(markerId, marker);
        incrementPlayerCount(ownerId);
        setDirty();

        broadcastToAll(new CreatePointMarkerPacket(markerId, ownerId, position, color, teamName, lifetimeTicks, targetType, entityName));
        broadcastMarkerNotification(marker, playerName, targetType, entityName);
        return marker;
    }

    public PathMarker createPathStart(UUID ownerId, Vec3 startPos, int color, String teamName, String playerName) {
        if (getPlayerMarkerCount(ownerId) >= Config.MAX_MARKERS_PER_PLAYER.get()) {
            return null;
        }

        UUID markerId = UUID.randomUUID();
        int lifetimeTicks = Config.MARKER_LIFETIME_SECONDS.get() * 20;
        PathMarker marker = new PathMarker(markerId, ownerId, startPos, null, (float) startPos.y, color, teamName, lifetimeTicks);
        pathMarkers.put(markerId, marker);
        incrementPlayerCount(ownerId);
        setDirty();

        broadcastToAll(new CreatePathMarkerPacket(markerId, ownerId, startPos, null, (float) startPos.y, color, teamName, lifetimeTicks, true, 0));
        return marker;
    }

    public void completePathMarker(UUID markerId, Vec3 endPos, String playerName) {
        PathMarker marker = pathMarkers.get(markerId);
        if (marker != null) {
            marker.setEndPos(endPos);
            setDirty();

            // 计算航向角度
            Vec3 dir = endPos.subtract(marker.getStartPos());
            float angle = (float) Math.toDegrees(Math.atan2(dir.z, dir.x));
            if (angle < 0) angle += 360;

            broadcastToAll(new CreatePathMarkerPacket(markerId, marker.getOwnerId(), marker.getStartPos(), endPos,
                    marker.getHeight(), marker.getColor(), marker.getTeamName(), marker.getRemainingTicks(), false, angle));
            broadcastPathNotification(marker, playerName, angle);
        }
    }

    public void clearMarkersByOwner(UUID ownerId) {
        pointMarkers.values().removeIf(m -> {
            if (m.getOwnerId().equals(ownerId)) {
                decrementPlayerCount(ownerId);
                return true;
            }
            return false;
        });

        pathMarkers.values().removeIf(m -> {
            if (m.getOwnerId().equals(ownerId)) {
                decrementPlayerCount(ownerId);
                return true;
            }
            return false;
        });

        setDirty();
        broadcastToAll(new ClearMarkersPacket(ownerId));
    }

    public void tick() {
        boolean changed = false;

        Iterator<PointMarker> pointIter = pointMarkers.values().iterator();
        while (pointIter.hasNext()) {
            PointMarker marker = pointIter.next();
            if (marker.tick()) {
                pointIter.remove();
                decrementPlayerCount(marker.getOwnerId());
                changed = true;
            }
        }

        Iterator<PathMarker> pathIter = pathMarkers.values().iterator();
        while (pathIter.hasNext()) {
            PathMarker marker = pathIter.next();
            if (marker.tick()) {
                pathIter.remove();
                decrementPlayerCount(marker.getOwnerId());
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        List<SyncMarkersPacket.PointMarkerData> pointData = new ArrayList<>();
        for (PointMarker marker : pointMarkers.values()) {
            pointData.add(new SyncMarkersPacket.PointMarkerData(
                    marker.getMarkerId(), marker.getOwnerId(), marker.getPosition(),
                    marker.getColor(), marker.getTeamName(), marker.getRemainingTicks()
            ));
        }

        List<SyncMarkersPacket.PathMarkerData> pathData = new ArrayList<>();
        for (PathMarker marker : pathMarkers.values()) {
            pathData.add(new SyncMarkersPacket.PathMarkerData(
                    marker.getMarkerId(), marker.getOwnerId(), marker.getStartPos(), marker.getEndPos(),
                    marker.getHeight(), marker.getColor(), marker.getTeamName(), marker.getRemainingTicks()
            ));
        }

        NetworkHandler.CHANNEL.sendTo(new SyncMarkersPacket(pointData, pathData), player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }

    private int getPlayerMarkerCount(UUID ownerId) {
        return playerMarkerCount.getOrDefault(ownerId, 0);
    }

    private void incrementPlayerCount(UUID ownerId) {
        playerMarkerCount.put(ownerId, getPlayerMarkerCount(ownerId) + 1);
    }

    private void decrementPlayerCount(UUID ownerId) {
        int count = getPlayerMarkerCount(ownerId);
        if (count > 1) {
            playerMarkerCount.put(ownerId, count - 1);
        } else {
            playerMarkerCount.remove(ownerId);
        }
    }

    private void broadcastToAll(Object packet) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                NetworkHandler.CHANNEL.sendTo(packet, player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
            }
        }
    }

    private void broadcastMarkerNotification(PointMarker marker, String playerName, int targetType, String entityName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // 构建颜色代码
        String hexColor = String.format("#%06X", marker.getColor() & 0xFFFFFF);
        String teamPrefix = marker.getTeamName().isEmpty() ? "" : "[" + marker.getTeamName() + "]";

        // 构建悬浮提示内容
        String hoverText;
        if (targetType == CreatePointMarkerPacket.TARGET_ENTITY && !entityName.isEmpty()) {
            hoverText = "实体: " + entityName;
        } else {
            Vec3 pos = marker.getPosition();
            hoverText = String.format("[%.1f, %.1f, %.1f]", pos.x, pos.y, pos.z);
        }

        // 发送给所有玩家
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (shouldPlayerSeeMarker(player, marker.getOwnerId(), marker.getTeamName())) {
                sendMarkerMessage(player, hexColor, teamPrefix, playerName, hoverText);
            }
        }
    }

    private void broadcastPathNotification(PathMarker marker, String playerName, float headingAngle) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        String hexColor = String.format("#%06X", marker.getColor() & 0xFFFFFF);
        String teamPrefix = marker.getTeamName().isEmpty() ? "" : "[" + marker.getTeamName() + "]";

        Vec3 start = marker.getStartPos();
        Vec3 end = marker.getEndPos();
        String hoverText = String.format("[%.1f, %.1f, %.1f] -> [%.1f, %.1f, %.1f] 航向%.0f°",
                start.x, start.y, start.z, end.x, end.y, end.z, headingAngle);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (shouldPlayerSeeMarker(player, marker.getOwnerId(), marker.getTeamName())) {
                sendMarkerMessage(player, hexColor, teamPrefix, playerName, hoverText);
            }
        }
    }

    private boolean shouldPlayerSeeMarker(ServerPlayer player, UUID ownerId, String markerTeamName) {
        if (ownerId.equals(player.getUUID())) return true;
        if (!Config.SHOW_ONLY_MY_TEAM.get()) return true;

        var localTeam = player.getTeam();
        if (localTeam == null) return true;

        if (markerTeamName == null || markerTeamName.isEmpty()) {
            return Config.SHOW_UNTEAM_MARKERS.get();
        }

        return localTeam.getName().equals(markerTeamName);
    }

    private void sendMarkerMessage(ServerPlayer player, String hexColor, String teamPrefix, String playerName, String hoverText) {
        // 使用原始JSON格式发送带悬浮提示的消息
        // 格式: [队名]玩家名 标记了一个位置
        // 悬浮提示放在队名+玩家名组合上
        // teamPrefix 已经包含方括号，如 "[TeamName]"
        String fullName = teamPrefix.isEmpty() ? playerName : teamPrefix + playerName;

        String message = String.format("[{\"text\":\"%s\",\"color\":\"%s\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"%s\"}},{\"text\":\" 标记了一个位置\",\"color\":\"white\"}]",
                fullName.replace("\"", "\\\""), hexColor, hoverText.replace("\"", "\\\""));

        player.sendSystemMessage(net.minecraft.network.chat.Component.Serializer.fromJson(message));
    }

    public Collection<PointMarker> getPointMarkers() {
        return Collections.unmodifiableCollection(pointMarkers.values());
    }

    public Collection<PathMarker> getPathMarkers() {
        return Collections.unmodifiableCollection(pathMarkers.values());
    }

    public PathMarker getPathMarker(UUID markerId) {
        return pathMarkers.get(markerId);
    }
}
