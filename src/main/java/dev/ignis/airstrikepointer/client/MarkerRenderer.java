package dev.ignis.airstrikepointer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.Config;
import dev.ignis.airstrikepointer.network.CreatePathMarkerPacket;
import dev.ignis.airstrikepointer.network.CreatePointMarkerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.*;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, value = Dist.CLIENT)
public class MarkerRenderer {
    private static final ResourceLocation POINT_TEXTURE = new ResourceLocation(AirstrikePointers.MODID, "textures/marker/point.png");
    private static final ResourceLocation PATH_TEXTURE = new ResourceLocation(AirstrikePointers.MODID, "textures/marker/path.png");

    private static final Map<UUID, ClientPointMarker> pointMarkers = new HashMap<>();
    private static final Map<UUID, ClientPathMarker> pathMarkers = new HashMap<>();

    public static void addPointMarker(CreatePointMarkerPacket packet) {
        if (shouldShowMarker(packet.ownerId(), packet.teamName())) {
            pointMarkers.put(packet.markerId(), new ClientPointMarker(packet));
        }
    }

    public static void addPathMarker(CreatePathMarkerPacket packet) {
        if (shouldShowMarker(packet.ownerId(), packet.teamName())) {
            pathMarkers.put(packet.markerId(), new ClientPathMarker(packet));
        }
    }

    public static void clearMarkersByOwner(UUID ownerId) {
        pointMarkers.values().removeIf(m -> m.ownerId.equals(ownerId));
        pathMarkers.values().removeIf(m -> m.ownerId.equals(ownerId));
    }

    public static void clearAllMarkers() {
        pointMarkers.clear();
        pathMarkers.clear();
    }

    private static boolean shouldShowMarker(UUID ownerId, String teamName) {
        Minecraft mc = Minecraft.getInstance();
        Player localPlayer = mc.player;
        if (localPlayer == null) return true;

        if (ownerId.equals(localPlayer.getUUID())) return true;

        if (!Config.SHOW_ONLY_MY_TEAM.get()) return true;

        var localTeam = localPlayer.getTeam();
        if (localTeam == null) return true;

        if (teamName == null || teamName.isEmpty()) {
            return Config.SHOW_UNTEAM_MARKERS.get();
        }

        return localTeam.getName().equals(teamName);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        pointMarkers.values().removeIf(ClientPointMarker::tick);
        pathMarkers.values().removeIf(ClientPathMarker::tick);
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        poseStack.pushPose();
        // 关键：将坐标系原点移动到相机位置，这样世界坐标就相对于相机了
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        for (ClientPointMarker marker : pointMarkers.values()) {
            renderPointMarker(poseStack, bufferSource, marker);
        }

        bufferSource.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void renderPointMarker(PoseStack poseStack, MultiBufferSource bufferSource, ClientPointMarker marker) {
        float size = 0.5f;

        // 现在可以直接使用世界坐标，因为坐标系已经被平移到相机位置
        float x = (float) marker.position.x;
        float y = (float) (marker.position.y + 1.0);
        float z = (float) marker.position.z;

        float r = ((marker.color >> 16) & 0xFF) / 255f;
        float g = ((marker.color >> 8) & 0xFF) / 255f;
        float b = (marker.color & 0xFF) / 255f;

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        // 绘制立方体线框
        // 底面
        line(vertexConsumer, poseStack, x - size, y - size, z - size, x + size, y - size, z - size, r, g, b, 1.0f);
        line(vertexConsumer, poseStack, x + size, y - size, z - size, x + size, y - size, z + size, r, g, b, 1.0f);
        line(vertexConsumer, poseStack, x + size, y - size, z + size, x - size, y - size, z + size, r, g, b, 1.0f);
        line(vertexConsumer, poseStack, x - size, y - size, z + size, x - size, y - size, z - size, r, g, b, 1.0f);
        // 顶面
        line(vertexConsumer, poseStack, x - size, y + size, z - size, x + size, y + size, z - size, r, g, b, 1.0f);
        line(vertexConsumer, poseStack, x + size, y + size, z - size, x + size, y + size, z + size, r, g, b, 1.0f);
        line(vertexConsumer, poseStack, x + size, y + size, z + size, x - size, y + size, z + size, r, g, b, 1.0f);
        line(vertexConsumer, poseStack, x - size, y + size, z + size, x - size, y + size, z - size, r, g, b, 1.0f);
        // 竖直边
        line(vertexConsumer, poseStack, x - size, y - size, z - size, x - size, y + size, z - size, r, g, b, 1.0f);
        line(vertexConsumer, poseStack, x + size, y - size, z - size, x + size, y + size, z - size, r, g, b, 1.0f);
        line(vertexConsumer, poseStack, x + size, y - size, z + size, x + size, y + size, z + size, r, g, b, 1.0f);
        line(vertexConsumer, poseStack, x - size, y - size, z + size, x - size, y + size, z + size, r, g, b, 1.0f);
    }

    private static void line(VertexConsumer consumer, PoseStack poseStack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        consumer.vertex(poseStack.last().pose(), x1, y1, z1).color(r, g, b, a).normal(poseStack.last().normal(), 0, 1, 0).endVertex();
        consumer.vertex(poseStack.last().pose(), x2, y2, z2).color(r, g, b, a).normal(poseStack.last().normal(), 0, 1, 0).endVertex();
    }

    private static void renderPointMarkerDirect(BufferBuilder buffer, ClientPointMarker marker, Vec3 cameraPos, Matrix4f matrix) {
        // 使用世界坐标相对于相机的偏移
        float x = (float) (marker.position.x - cameraPos.x);
        float y = (float) (marker.position.y - cameraPos.y + 1.0);
        float z = (float) (marker.position.z - cameraPos.z);
        float size = 0.5f;

        int r = (marker.color >> 16) & 0xFF;
        int g = (marker.color >> 8) & 0xFF;
        int b = marker.color & 0xFF;
        int a = 255;

        // 线框的12条边
        // 底面
        line(buffer, matrix, x - size, y - size, z - size, x + size, y - size, z - size, r, g, b, a);
        line(buffer, matrix, x + size, y - size, z - size, x + size, y - size, z + size, r, g, b, a);
        line(buffer, matrix, x + size, y - size, z + size, x - size, y - size, z + size, r, g, b, a);
        line(buffer, matrix, x - size, y - size, z + size, x - size, y - size, z - size, r, g, b, a);
        // 顶面
        line(buffer, matrix, x - size, y + size, z - size, x + size, y + size, z - size, r, g, b, a);
        line(buffer, matrix, x + size, y + size, z - size, x + size, y + size, z + size, r, g, b, a);
        line(buffer, matrix, x + size, y + size, z + size, x - size, y + size, z + size, r, g, b, a);
        line(buffer, matrix, x - size, y + size, z + size, x - size, y + size, z - size, r, g, b, a);
        // 竖直边
        line(buffer, matrix, x - size, y - size, z - size, x - size, y + size, z - size, r, g, b, a);
        line(buffer, matrix, x + size, y - size, z - size, x + size, y + size, z - size, r, g, b, a);
        line(buffer, matrix, x + size, y - size, z + size, x + size, y + size, z + size, r, g, b, a);
        line(buffer, matrix, x - size, y - size, z + size, x - size, y + size, z + size, r, g, b, a);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }

    private static void renderPointMarker(PoseStack poseStack, ClientPointMarker marker, Vec3 cameraPos) {
        RenderSystem.setShaderTexture(0, POINT_TEXTURE);

        Vec3 pos = marker.position;
        float floatOffset = (float) Math.sin((marker.age + Minecraft.getInstance().getFrameTime()) * 0.1) * 0.1f;

        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y + 1.5 + floatOffset, pos.z);

        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.5f, 0.5f, 0.5f);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        int r = (marker.color >> 16) & 0xFF;
        int g = (marker.color >> 8) & 0xFF;
        int b = marker.color & 0xFF;
        int alpha = 200;

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(matrix, -1, -1, 0).uv(0, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, -1, 0).uv(1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 0).uv(1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, -1, 1, 0).uv(0, 0).color(r, g, b, alpha).endVertex();
        tesselator.end();

        poseStack.popPose();
    }

    private static void renderPathMarker(PoseStack poseStack, ClientPathMarker marker, Vec3 cameraPos) {
        if (marker.endPos == null) return;

        RenderSystem.setShaderTexture(0, PATH_TEXTURE);

        // 计算相对于相机的位置
        double startRx = marker.startPos.x - cameraPos.x;
        double startRz = marker.startPos.z - cameraPos.z;
        double endRx = marker.endPos.x - cameraPos.x;
        double endRz = marker.endPos.z - cameraPos.z;
        double ry = marker.height - cameraPos.y;

        Vec3 dir = marker.endPos.subtract(marker.startPos);
        float length = (float) dir.horizontalDistance();
        if (length < 0.01f) return;

        Vec3 norm = dir.normalize();
        float angle = (float) Math.atan2(norm.z, norm.x);

        poseStack.pushPose();
        poseStack.translate(startRx, ry, startRz);
        poseStack.mulPose(new Quaternionf().rotateY(-angle + (float) Math.PI / 2));

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        int r = (marker.color >> 16) & 0xFF;
        int g = (marker.color >> 8) & 0xFF;
        int b = marker.color & 0xFF;
        int alpha = 180;

        float width = 0.5f;
        float uOffset = (marker.age % 100) / 100f;

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        buffer.vertex(matrix, -width, 0.05f, 0).uv(0 + uOffset, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, -width, 0.05f, length).uv(length + uOffset, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, width, 0.05f, length).uv(length + uOffset, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, width, 0.05f, 0).uv(0 + uOffset, 0).color(r, g, b, alpha).endVertex();

        tesselator.end();
        poseStack.popPose();
    }

    private static class ClientPointMarker {
        final UUID markerId;
        final UUID ownerId;
        final Vec3 position;
        final int color;
        final String teamName;
        int remainingTicks;
        int age;

        ClientPointMarker(CreatePointMarkerPacket packet) {
            this.markerId = packet.markerId();
            this.ownerId = packet.ownerId();
            this.position = packet.position();
            this.color = packet.color();
            this.teamName = packet.teamName();
            this.remainingTicks = packet.lifetimeTicks();
            this.age = 0;
        }

        boolean tick() {
            remainingTicks--;
            age++;
            return remainingTicks <= 0;
        }
    }

    private static class ClientPathMarker {
        final UUID markerId;
        final UUID ownerId;
        final Vec3 startPos;
        Vec3 endPos;
        final float height;
        final int color;
        final String teamName;
        int remainingTicks;
        int age;

        ClientPathMarker(CreatePathMarkerPacket packet) {
            this.markerId = packet.markerId();
            this.ownerId = packet.ownerId();
            this.startPos = packet.startPos();
            this.endPos = packet.endPos();
            this.height = packet.height();
            this.color = packet.color();
            this.teamName = packet.teamName();
            this.remainingTicks = packet.lifetimeTicks();
            this.age = 0;
        }

        boolean tick() {
            remainingTicks--;
            age++;
            return remainingTicks <= 0;
        }
    }
}
