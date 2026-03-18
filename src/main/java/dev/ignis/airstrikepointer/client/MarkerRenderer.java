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
import org.lwjgl.opengl.GL11;

import java.util.*;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, value = Dist.CLIENT)
public class MarkerRenderer {
    @SuppressWarnings("removal")
    private static final ResourceLocation POINT_TEXTURE = new ResourceLocation(AirstrikePointers.MODID, "textures/marker/point.png");
    @SuppressWarnings("removal")
    private static final ResourceLocation PATH_TEXTURE = new ResourceLocation(AirstrikePointers.MODID, "textures/marker/path.png");

    // 自定义RenderType：无深度测试（穿墙显示）
    private static final RenderType MARKER_RENDER_TYPE = createMarkerRenderType(POINT_TEXTURE);
    private static final RenderType PATH_RENDER_TYPE = createMarkerRenderType(PATH_TEXTURE);

    private static RenderType createMarkerRenderType(ResourceLocation texture) {
        return RenderType.create(
                "airstrike_marker",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new net.minecraft.client.renderer.RenderStateShard.ShaderStateShard(
                                net.minecraft.client.renderer.GameRenderer::getRendertypeTextSeeThroughShader))
                        .setTextureState(new net.minecraft.client.renderer.RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(new net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard(
                                "translucent_transparency", () -> {
                                    RenderSystem.enableBlend();
                                    RenderSystem.defaultBlendFunc();
                                }, RenderSystem::disableBlend))
                        .setDepthTestState(new net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard("always", 519))
                        .setCullState(new net.minecraft.client.renderer.RenderStateShard.CullStateShard(false))
                        .setLightmapState(new net.minecraft.client.renderer.RenderStateShard.LightmapStateShard(true))
                        .setOverlayState(new net.minecraft.client.renderer.RenderStateShard.OverlayStateShard(true))
                        .setWriteMaskState(new net.minecraft.client.renderer.RenderStateShard.WriteMaskStateShard(true, true))
                        .createCompositeState(false)
        );
    }

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

        // 渲染路径标记（在点标记之前，避免遮挡）
        for (ClientPathMarker marker : pathMarkers.values()) {
            renderPathMarker(poseStack, bufferSource, marker);
        }

        // 渲染坐标点标记
        for (ClientPointMarker marker : pointMarkers.values()) {
            renderPointMarker(poseStack, bufferSource, marker);
        }

        bufferSource.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void renderPointMarker(PoseStack poseStack, MultiBufferSource bufferSource, ClientPointMarker marker) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        double distance = marker.position.distanceTo(cameraPos);

        // 距离自适应大小：越远越大，但有最小值
        float minDistance = 32.0f;
        float maxDistance = 640.0f;
        float minScale = 1f;
        float maxScale = 5f;

        float distanceFactor = (float) Math.min(1.0, Math.max(0.0, (distance - minDistance) / (maxDistance - minDistance)));
        float scale = minScale + (maxScale - minScale) * distanceFactor;

        float x = (float) marker.position.x;
        float y = (float) (marker.position.y + 0.5);
        float z = (float) marker.position.z;

        float r = ((marker.color >> 16) & 0xFF) / 255f;
        float g = ((marker.color >> 8) & 0xFF) / 255f;
        float b = (marker.color & 0xFF) / 255f;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // Billboard：使面片朝向相机
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(scale, scale, scale);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(MARKER_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();

        // 绘制面片
        float alpha = 0.8f;
        vertex(vertexConsumer, pose, -1, -1, 0, 0, 1, r, g, b, alpha);
        vertex(vertexConsumer, pose, 1, -1, 0, 1, 1, r, g, b, alpha);
        vertex(vertexConsumer, pose, 1, 1, 0, 1, 0, r, g, b, alpha);
        vertex(vertexConsumer, pose, -1, 1, 0, 0, 0, r, g, b, alpha);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0, 1, 0)
                .endVertex();
    }

    private static void renderPathMarker(PoseStack poseStack, MultiBufferSource bufferSource, ClientPathMarker marker) {
        if (marker.endPos == null) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // 计算路径中点到相机的距离用于自适应宽度
        Vec3 midPoint = marker.startPos.add(marker.endPos).scale(0.5);
        double distance = midPoint.distanceTo(cameraPos);

        // 距离自适应宽度：64m以下5格，64+640m时25格
        float minDistance = 64.0f;
        float maxDistance = 704.0f; // 64 + 640
        float minWidth = 5.0f;
        float maxWidth = 25.0f;

        float distanceFactor = (float) Math.min(1.0, Math.max(0.0, (distance - minDistance) / (maxDistance - minDistance)));
        float width = minWidth + (maxWidth - minWidth) * distanceFactor;

        float startX = (float) marker.startPos.x;
        float startZ = (float) marker.startPos.z;
        float y = marker.height;

        Vec3 dir = marker.endPos.subtract(marker.startPos);
        float length = (float) dir.horizontalDistance();
        if (length < 0.01f) return;

        float r = ((marker.color >> 16) & 0xFF) / 255f;
        float g = ((marker.color >> 8) & 0xFF) / 255f;
        float b = (marker.color & 0xFF) / 255f;

        poseStack.pushPose();
        poseStack.translate(startX, y, startZ);

        // 计算旋转角度，使路径指向终点
        Vec3 norm = dir.normalize();
        float angle = (float) Math.atan2(norm.z, norm.x);
        poseStack.mulPose(new Quaternionf().rotateY(-angle + (float) Math.PI / 2));

        VertexConsumer vertexConsumer = bufferSource.getBuffer(PATH_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();

        // 翻页动画偏移
        float uOffset = -(marker.age % 100) / 100f; // 负号使动画方向正确
        float alpha = 0.7f;

        // 绘制平面路径条带 - 修复UV坐标
        float u1 = uOffset;
        float u2 = length * 0.1f + uOffset; // 缩放UV避免过度重复
        vertex(vertexConsumer, pose, -width, 0.05f, 0, u1, 1, r, g, b, alpha);
        vertex(vertexConsumer, pose, -width, 0.05f, length, u2, 1, r, g, b, alpha);
        vertex(vertexConsumer, pose, width, 0.05f, length, u2, 0, r, g, b, alpha);
        vertex(vertexConsumer, pose, width, 0.05f, 0, u1, 0, r, g, b, alpha);

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
