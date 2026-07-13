package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.markers.MarkerStorage;
import dev.ignis.airstrikepointer.markers.PointMarkerIcon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端→服务端：袖珍激光笔轮盘标记请求。
 * 玩家通过轮盘选择图标后，客户端发送此包通知服务端创建标记。
 */
public record CreatePocketMarkerC2SPacket(
        Vec3 position,
        int targetType,
        String entityName,
        UUID targetEntityId,
        String iconId
) {
    public static final int TARGET_MISS = 0;
    public static final int TARGET_BLOCK = 1;
    public static final int TARGET_ENTITY = 2;

    private static final int[] MARKER_COLORS = {
            0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF,
            0xFF55FF, 0xFF8800, 0x00FF88, 0xFF0088, 0x88FF00, 0x0088FF
    };

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeInt(targetType);
        buf.writeUtf(entityName != null ? entityName : "");
        buf.writeBoolean(targetEntityId != null);
        if (targetEntityId != null) {
            buf.writeUUID(targetEntityId);
        }
        buf.writeUtf(iconId != null ? iconId : "");
    }

    public static CreatePocketMarkerC2SPacket decode(FriendlyByteBuf buf) {
        Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int targetType = buf.readInt();
        String entityName = buf.readUtf();
        if (entityName.isEmpty()) entityName = "";
        UUID targetEntityId = buf.readBoolean() ? buf.readUUID() : null;
        String iconId = buf.readUtf();
        if (iconId.isEmpty()) iconId = PointMarkerIcon.ICON_DOT_ID;
        return new CreatePocketMarkerC2SPacket(position, targetType, entityName, targetEntityId, iconId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            MarkerStorage storage = MarkerStorage.get(level);

            // 验证图标有效性
            if (!PointMarkerIcon.isValid(iconId)) {
                return;
            }

            // 标点前清除该玩家之前的所有无制导点
            storage.clearNonGuidedMarkersByOwner(player.getUUID());

            int color = getPlayerColor(player);
            String teamName = getPlayerTeamName(player);

            var marker = storage.createPointMarker(
                    player.getUUID(), position, color, teamName,
                    targetType, entityName, targetEntityId,
                    player.getDisplayName().getString(),
                    null, null, null, iconId,
                    true // guidanceDisabled=true，不可制导
            );

            if (marker != null) {
                player.displayClientMessage(
                        Component.translatable("message.airstrikepointers.point_marked")
                                .withStyle(ChatFormatting.GREEN), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BIT.value(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                player.displayClientMessage(
                        Component.translatable("message.airstrikepointers.marker_limit_reached")
                                .withStyle(ChatFormatting.RED), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static int getPlayerColor(ServerPlayer player) {
        Team team = player.getTeam();
        if (team != null && team.getColor() != ChatFormatting.RESET) {
            Integer teamColor = team.getColor().getColor();
            if (teamColor != null) return teamColor;
        }
        int index = Math.abs(player.getUUID().hashCode()) % MARKER_COLORS.length;
        return MARKER_COLORS[index];
    }

    private static String getPlayerTeamName(ServerPlayer player) {
        Team team = player.getTeam();
        return team != null ? team.getName() : "";
    }
}
