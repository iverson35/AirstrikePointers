package dev.ignis.airstrikepointer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.Config;
import dev.ignis.airstrikepointer.items.ModItems;
import dev.ignis.airstrikepointer.markers.PointMarkerIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 袖珍激光笔长按右键时显示的标记轮盘。
 * 8 个 icon_* 图标沿圆周排列，中央为 icon_dot。
 * 玩家移动鼠标选择图标，松开右键时创建对应标记。
 */
@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MarkerWheelOverlay {

    // 轮盘图标顺序（逆时针，从正上方开始）
    private static final String[] WHEEL_ICONS = {
            PointMarkerIcon.ICON_DANGER_ID,    // 0°   正上方
            PointMarkerIcon.ICON_CAUTION_ID,   // 45°  右上
            PointMarkerIcon.ICON_SATISFIED_ID, // 90°  正右
            PointMarkerIcon.ICON_LIKE_ID,      // 135° 右下
            PointMarkerIcon.ICON_ATTACK_ID,    // 180° 正下
            PointMarkerIcon.ICON_DEFEND_ID,    // 225° 左下
            PointMarkerIcon.ICON_PUZZLED_ID,   // 270° 正左
            PointMarkerIcon.ICON_REFUSE_ID,    // 315° 左上
    };

    private static final int ICON_COUNT = WHEEL_ICONS.length; // 8
    private static final float WHEEL_RADIUS = 70.0f;  // 圆周半径
    private static final float CENTER_DEAD_ZONE = 8.0f; // 中心死区半径（在此范围内选中 dot）
    private static final int ICON_SIZE = 20; // 渲染图标大小
    private static final int ICON_SIZE_SELECTED = 26; // 选中图标放大
    private static final float ANGLE_PER_ICON = 360.0f / ICON_COUNT; // 45°
    private static final ResourceLocation CURSOR_TEXTURE = new ResourceLocation(AirstrikePointers.MODID, "textures/gui/wheel_cursor.png");
    private static final int CURSOR_SIZE = 16;

    // 轮盘显示阈值（ticks），从客户端配置读取，默认 3 ticks
    private static int getWheelThresholdTicks() {
        return Config.WHEEL_HOLD_THRESHOLD_TICKS.get();
    }

    // 状态
    private static boolean wheelActive = false;
    private static int selectedIndex = -1; // -1 = center dot, 0-7 = outer icons
    private static int holdTicks = 0;

    // 轮盘激活时的鼠标原点（用于增量选择，而非绝对坐标）
    private static double wheelOriginX = 0;
    private static double wheelOriginY = 0;
    private static boolean originCaptured = false;

    // 右键按下时捕获的目标（解决选图标和瞄准的矛盾）
    private static Vec3 capturedTargetPos = null;
    private static int capturedTargetType = 0; // 0=miss, 1=block, 2=entity
    private static String capturedEntityName = "";
    private static UUID capturedEntityId = null;

    /**
     * 每 tick 更新轮盘状态。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            reset();
            return;
        }

        // 检查是否正在使用袖珍激光笔
        boolean holdingPocketLaser = mc.player.isUsingItem()
                && mc.player.getUseItem().is(ModItems.POCKET_LASER_POINTER.get());

        if (!holdingPocketLaser) {
            reset();
            return;
        }

        // 计算已持有时长
        int useDuration = mc.player.getUseItem().getUseDuration();
        int timeLeft = mc.player.getUseItemRemainingTicks();
        holdTicks = useDuration - timeLeft;

        if (holdTicks >= getWheelThresholdTicks()) {
            if (!wheelActive) {
                // 轮盘刚激活：播放音效
                mc.player.playSound(SoundEvents.LEVER_CLICK, 1.0F, 1.0F);
            }
            wheelActive = true;
            // 根据鼠标相对于原点的偏移量更新选中项（原点在 use() 时已捕获）
            updateSelection(mc);
        } else {
            wheelActive = false;
            selectedIndex = -1;
        }
    }

    /**
     * 根据鼠标相对于轮盘激活原点的偏移量更新选中的图标。
     * 使用增量而非绝对坐标，确保轮盘打开时默认选中中央 dot。
     */
    private static void updateSelection(Minecraft mc) {
        if (mc.getWindow() == null || !originCaptured) return;

        // xpos/ypos 已经是 GUI 坐标，无需额外缩放
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        // 使用鼠标相对于激活原点的偏移量
        double dx = mouseX - wheelOriginX;
        double dy = mouseY - wheelOriginY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        // 中心死区：选中 dot
        if (dist < CENTER_DEAD_ZONE) {
            selectedIndex = -1;
            return;
        }

        // 计算角度（0° = 正上方，顺时针）
        // Minecraft GUI: Y 轴向下，所以用 atan2(-dy, dx) 得到标准角度
        double angleDeg = Math.toDegrees(Math.atan2(-dy, dx));
        // 转换成 0° = 正上方，顺时针
        angleDeg = 90 - angleDeg;
        if (angleDeg < 0) angleDeg += 360;
        if (angleDeg >= 360) angleDeg -= 360;

        // 计算对应的扇形索引
        // 扇形 0 对应 -ANGLE_PER_ICON/2 到 +ANGLE_PER_ICON/2 (即 337.5° ~ 22.5°)
        float sectorAngle = (float) angleDeg + ANGLE_PER_ICON / 2.0f;
        if (sectorAngle >= 360) sectorAngle -= 360;
        int index = (int) (sectorAngle / ANGLE_PER_ICON);
        selectedIndex = Math.min(index, ICON_COUNT - 1);
    }

    /**
     * 渲染轮盘到 GUI 覆盖层。
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!wheelActive) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = gui.guiWidth();
        int screenHeight = gui.guiHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        var pose = gui.pose();
        pose.pushPose();

        // 绘制鼠标判定位置指示点（相对偏移量映射到屏幕中心）
        if (originCaptured) {
            double dx = mc.mouseHandler.xpos() - wheelOriginX;
            double dy = mc.mouseHandler.ypos() - wheelOriginY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            float maxDist = WHEEL_RADIUS + 14.0f;
            if (dist > maxDist) {
                dx = dx / dist * maxDist;
                dy = dy / dist * maxDist;
            }
            int dotX = centerX + (int) dx;
            int dotY = centerY + (int) dy;
            renderIcon(gui, CURSOR_TEXTURE, dotX - CURSOR_SIZE / 2, dotY - CURSOR_SIZE / 2, CURSOR_SIZE, 0.7f);
        }

        // 绘制 8 个外围图标
        for (int i = 0; i < ICON_COUNT; i++) {
            boolean isSelected = (i == selectedIndex);
            float angle = (float) Math.toRadians(i * ANGLE_PER_ICON - 90);
            int ix = centerX + (int) (Math.cos(angle) * WHEEL_RADIUS);
            int iy = centerY + (int) (Math.sin(angle) * WHEEL_RADIUS);
            int size = isSelected ? ICON_SIZE_SELECTED : ICON_SIZE;
            float alpha = isSelected ? 1.0f : 0.7f;

            ResourceLocation texture = PointMarkerIcon.getTexture(WHEEL_ICONS[i]);
            renderIcon(gui, texture, ix - size / 2, iy - size / 2, size, alpha);

            // 选中时绘制高亮边框
            if (isSelected) {
                gui.fill(ix - size / 2 - 2, iy - size / 2 - 2, ix + size / 2 + 2, iy - size / 2, 0xFFFFFFFF);
                gui.fill(ix - size / 2 - 2, iy + size / 2, ix + size / 2 + 2, iy + size / 2 + 2, 0xFFFFFFFF);
                gui.fill(ix - size / 2 - 2, iy - size / 2, ix - size / 2, iy + size / 2, 0xFFFFFFFF);
                gui.fill(ix + size / 2, iy - size / 2, ix + size / 2 + 2, iy + size / 2, 0xFFFFFFFF);
            }
        }

        // 绘制选中的图标名称标签
        Component labelText = getSelectedLabel();
        if (!labelText.getString().isEmpty()) {
            int labelWidth = mc.font.width(labelText);
            gui.drawCenteredString(mc.font, labelText, centerX, centerY + 14, 0xFFFFFF);
        }

        pose.popPose();
    }

    /**
     * 渲染图标纹理。
     */
    private static void renderIcon(GuiGraphics gui, ResourceLocation texture, int x, int y, int size, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        gui.blit(texture, x, y, 0, 0, 0, size, size, size, size);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    /**
     * 右键按下时捕获鼠标原点，用于后续轮盘的增量选择。
     * 在 use() 中由 PocketLaserPointerItem 调用，早于轮盘激活。
     * 这样玩家可以在轮盘出现前就开始移动鼠标，松手即选。
     */
    public static void captureOrigin() {
        Minecraft mc = Minecraft.getInstance();
        wheelOriginX = mc.mouseHandler.xpos();
        wheelOriginY = mc.mouseHandler.ypos();
        originCaptured = true;
    }

    /**
     * 右键按下时捕获目标。在 use() 中由 PocketLaserPointerItem 调用。
     * 这样玩家可以在之后移动视角选择图标，而不丢失瞄准的目标。
     */
    public static void captureTarget(Player player) {
        capturedTargetPos = null;
        capturedTargetType = 0;
        capturedEntityName = "";
        capturedEntityId = null;

        var level = player.level();
        Vec3 eyePos = player.getEyePosition(0.0f);
        Vec3 lookVec = player.getViewVector(0.0f);
        Vec3 endPos = eyePos.add(lookVec.x * 300.0, lookVec.y * 300.0, lookVec.z * 300.0);

        var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                level, player, eyePos, endPos,
                player.getBoundingBox().expandTowards(lookVec.scale(300.0)).inflate(1.0),
                entity -> !entity.isSpectator() && entity.isPickable()
        );

        if (entityHit != null) {
            Entity hitEntity = entityHit.getEntity();
            if (hitEntity instanceof PartEntity<?> partEntity) {
                hitEntity = partEntity.getParent();
            }
            capturedTargetPos = entityHit.getLocation();
            capturedTargetType = 2; // entity
            capturedEntityName = hitEntity.getDisplayName().getString();
            capturedEntityId = hitEntity.getUUID();
        } else {
            BlockHitResult blockHit = (BlockHitResult) player.pick(300.0, 0.0f, false);
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                capturedTargetPos = blockHit.getLocation();
                capturedTargetType = 1; // block
            } else {
                capturedTargetPos = endPos;
                capturedTargetType = 0; // miss
            }
        }
    }

    /** 获取捕获的目标位置 */
    public static Vec3 getCapturedPos() { return capturedTargetPos; }
    /** 获取捕获的目标类型 */
    public static int getCapturedType() { return capturedTargetType; }
    /** 获取捕获的实体名 */
    public static String getCapturedEntityName() { return capturedEntityName; }
    /** 获取捕获的实体 UUID */
    public static UUID getCapturedEntityId() { return capturedEntityId; }

    /**
     * 获取当前选中图标的显示标签。
     */
    private static Component getSelectedLabel() {
        String iconId;
        if (selectedIndex == -1) {
            iconId = PointMarkerIcon.ICON_DOT_ID;
        } else if (selectedIndex >= 0 && selectedIndex < ICON_COUNT) {
            iconId = WHEEL_ICONS[selectedIndex];
        } else {
            return Component.empty();
        }
        return Component.translatable("icon.airstrikepointers." + iconId);
    }

    // ===== 公共 API =====

    /** 轮盘是否正在显示 */
    public static boolean isWheelActive() {
        return wheelActive;
    }

    /** 获取当前选中的图标 ID，-1 代表中央 dot */
    public static String getSelectedIcon() {
        if (selectedIndex == -1 || selectedIndex >= ICON_COUNT) {
            return PointMarkerIcon.ICON_DOT_ID;
        }
        return WHEEL_ICONS[selectedIndex];
    }

    /** 重置轮盘状态 */
    public static void reset() {
        wheelActive = false;
        selectedIndex = -1;
        holdTicks = 0;
        originCaptured = false;
    }
}
