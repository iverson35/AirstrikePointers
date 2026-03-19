package dev.ignis.airstrikepointer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.items.LaserPointerItem;
import dev.ignis.airstrikepointer.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Math;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LaserPointerOverlayRenderer {

    @SuppressWarnings("removal")
    // 使用自定义纹理
    private static final ResourceLocation SCOPE_LOCATION = new ResourceLocation(AirstrikePointers.MODID, "textures/gui/laser_pointer_scope.png");
    
    // 视野缩放动画状态
    private static final float INITIAL_SCALE = 0.5F;
    private static final float TARGET_SCALE = 1.05F;
    private static float scopeScale = INITIAL_SCALE;
    
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        // 只在第一人称时生效
        if (!mc.options.getCameraType().isFirstPerson()) return;
        
        if (player == null || !player.isUsingItem()) return;
        
        if (!player.getUseItem().is(ModItems.LASER_POINTER.get())) return;
        
        // 取消原版HUD元素渲染
        if (isHudOverlay(event.getOverlay())) {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        // 只在第一人称时生效
        if (!mc.options.getCameraType().isFirstPerson()) return;
        
        if (player == null) return;
        
        if (!(player.isUsingItem() && player.getUseItem().is(ModItems.LASER_POINTER.get()))) {
            scopeScale = INITIAL_SCALE;
            return;
        }
        
        // 更新动画
        updateAnimation();
        
        // 渲染望远镜覆盖层
        renderSpyglassOverlay(event.getGuiGraphics());
        
        // 渲染目标信息（距离和实体名称）
        renderTargetInfo(event.getGuiGraphics(), player);
    }
    
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        // 只在第一人称时生效
        if (!mc.options.getCameraType().isFirstPerson()) return;
        
        if (player == null || !player.isUsingItem()) return;
        
        // 隐藏第一人称手部渲染
        if (player.getUseItem().is(ModItems.LASER_POINTER.get())) {
            event.setCanceled(true);
        }
    }
    
    // 在渲染时更新动画
    private static void updateAnimation() {
        Minecraft mc = Minecraft.getInstance();
        
        // 检查是否正在使用激光指示器且在第一人称
        boolean isUsingLaserPointer = mc.player != null && 
                                       mc.player.isUsingItem() && 
                                       mc.player.getUseItem().is(ModItems.LASER_POINTER.get()) &&
                                       mc.options.getCameraType().isFirstPerson();
        
        // 使用getDeltaFrameTime计算增量，在0.25秒内完成扩大
        // 总距离 = 1.125F - 0.5F = 0.625F
        // 每帧增量 = 总距离 / 0.25秒 * 帧时间补偿
        float deltaFrame = mc.getDeltaFrameTime();
        float totalDistance = TARGET_SCALE - INITIAL_SCALE; // 0.625F
        float step = (totalDistance / 0.25F) * deltaFrame * 0.002F;
        
        if (isUsingLaserPointer) {
            // 动画扩大到目标大小
            if (scopeScale < TARGET_SCALE) {
                scopeScale = Math.min(scopeScale + step, TARGET_SCALE);
            }
            // 调试输出
            //System.out.println("[AirstrikePointer] scopeScale: " + scopeScale + ", step: " + step);
        } else {
            // 重置为初始大小
            scopeScale = INITIAL_SCALE;
        }
    }
    
    private static boolean isHudOverlay(NamedGuiOverlay overlay) {
        return overlay.id().equals(VanillaGuiOverlay.HOTBAR.id()) || 
               overlay.id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) || 
               overlay.id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) || 
               overlay.id().equals(VanillaGuiOverlay.FOOD_LEVEL.id()) || 
               overlay.id().equals(VanillaGuiOverlay.AIR_LEVEL.id()) || 
               overlay.id().equals(VanillaGuiOverlay.MOUNT_HEALTH.id()) || 
               overlay.id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()) || 
               overlay.id().equals(VanillaGuiOverlay.JUMP_BAR.id()) || 
               overlay.id().equals(VanillaGuiOverlay.ITEM_NAME.id());
    }
    
    private static void renderSpyglassOverlay(GuiGraphics gui) {
        // 设置渲染状态 - 与原版望远镜一致
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        int screenWidth = gui.guiWidth();
        int screenHeight = gui.guiHeight();
        
        // 使用动画缩放值计算大小
        float baseSize = (float) Math.min(screenWidth, screenHeight);
        float renderSize = baseSize * easeOut(TARGET_SCALE,INITIAL_SCALE,scopeScale-INITIAL_SCALE);
        int x = (int) ((screenWidth - renderSize) / 2);
        int y = (int) ((screenHeight - renderSize) / 2);
        
        // 渲染望远镜纹理（调整透明度以匹配原版效果）
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.1F); // 降低整体透明度
        gui.blit(SCOPE_LOCATION, x, y, -90, 0.0F, 0.0F, (int) renderSize, (int) renderSize, (int) renderSize, (int) renderSize);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // 恢复
        
        // 四角黑色遮罩
        int x2 = (int) (x + renderSize);
        int y2 = (int) (y + renderSize);
        gui.fill(RenderType.guiOverlay(), 0, y2, screenWidth, screenHeight, -90, -16777216);
        gui.fill(RenderType.guiOverlay(), 0, 0, screenWidth, y, -90, -16777216);
        gui.fill(RenderType.guiOverlay(), 0, y, x, y2, -90, -16777216);
        gui.fill(RenderType.guiOverlay(), x2, y, screenWidth, y2, -90, -16777216);
    }

    private static float easeOut(float max, float min, float t) {
        // easeOutQuad: 先快后慢的缓动效果
        // t 应该在 0 到 1 之间
        float tmp = 1 - t;
        float ease = (float) (1 - java.lang.Math.pow(tmp, 5));
        return min + (max - min) * ease;
    }
    
    private static void renderTargetInfo(GuiGraphics gui, LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = gui.guiWidth();
        int screenHeight = gui.guiHeight();
        
        // 准星位置（屏幕中心）
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        // ===== 左侧：显示模式信息 =====
        ItemStack stack = player.getUseItem();
        LaserPointerItem.Mode mode = getLaserPointerMode(stack);
        
        // 计算模式文本宽度，用于右对齐
        String modeKey = switch (mode) {
            case POINT -> "mode.airstrikepointers.point";
            case PATH -> "mode.airstrikepointers.path";
            case CLEAR -> "mode.airstrikepointers.clear";
        };
        String modeText = Component.translatable(modeKey).getString();
        int modeTextWidth = mc.font.width(modeText);
        
        // 左侧对称位置（与右侧距离信息对称）
        int modeX = centerX - 20 - modeTextWidth;
        int modeY = centerY - 10;
        
        // 模式颜色
        int modeColor = switch (mode) {
            case POINT -> 0x55FF55; // 绿色
            case PATH -> 0x5555FF; // 蓝色
            case CLEAR -> 0xFF5555; // 红色
        };
        
        // 渲染模式名称
        gui.drawString(mc.font, modeText, modeX, modeY, modeColor);
        
        // 路径模式下，根据状态显示不同的提示
        if (mode == LaserPointerItem.Mode.PATH) {
            boolean hasStartPoint = hasPathStartPoint(stack);
            boolean isUsing = player.isUsingItem();
            
            String hintKey;
            int hintColor;
            
            if (!hasStartPoint) {
                // 还没有起点，显示"标记起点"
                hintKey = "gui.airstrikepointers.mark_start";
                hintColor = 0x55FF55; // 绿色
            } else if (isUsing) {
                // 有起点且正在按住右键，显示"标记终点"
                hintKey = "gui.airstrikepointers.mark_end";
                hintColor = 0x55FF55; // 绿色
            } else {
                // 有起点但没按住右键，显示"标记起点"（提示可以重新标记起点）
                hintKey = "gui.airstrikepointers.mark_start";
                hintColor = 0x55FF55; // 绿色
            }
            
            String hintText = Component.translatable(hintKey).getString();
            int hintTextWidth = mc.font.width(hintText);
            int hintX = centerX - 20 - hintTextWidth;
            gui.drawString(mc.font, hintText, hintX, modeY + 12, hintColor);
        }
        
        // ===== 右侧：显示距离和实体信息 =====
        // 探测距离最大300格
        final double MAX_DISTANCE = 300.0;
        
        Vec3 eyePos = player.getEyePosition(0.0f);
        Vec3 lookVec = player.getViewVector(0.0f);
        Vec3 endPos = eyePos.add(lookVec.x * MAX_DISTANCE, lookVec.y * MAX_DISTANCE, lookVec.z * MAX_DISTANCE);
        
        // 首先检测实体
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(MAX_DISTANCE)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player.level(), player, eyePos, endPos, searchBox,
                entity -> !entity.isSpectator() && entity.isPickable()
        );
        
        Vec3 targetPos;
        Entity targetEntity = null;
        
        if (entityHit != null) {
            targetPos = entityHit.getLocation();
            targetEntity = entityHit.getEntity();
        } else {
            // 检测方块
            HitResult blockHit = player.pick(MAX_DISTANCE, 0.0f, false);
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                targetPos = blockHit.getLocation();
            } else {
                targetPos = endPos;
            }
        }
        
        // 计算距离
        double distance = eyePos.distanceTo(targetPos);
        
        // 信息位置：准星右上角偏移
        int infoX = centerX + 20;
        int infoY = centerY - 10;
        
        // 渲染距离信息
        String distanceText = String.format("%.1fm", distance);
        gui.drawString(mc.font, distanceText, infoX, infoY, 0xFFFFFF);
        
        // 如果是实体，额外显示displayname
        if (targetEntity != null) {
            String entityName = targetEntity.getDisplayName().getString();
            gui.drawString(mc.font, entityName, infoX, infoY + 12, 0xFFFF55); // 黄色
        }
    }
    
    private static LaserPointerItem.Mode getLaserPointerMode(ItemStack stack) {
        if (!stack.is(ModItems.LASER_POINTER.get())) {
            return LaserPointerItem.Mode.POINT;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Mode")) {
            return LaserPointerItem.Mode.POINT;
        }
        try {
            return LaserPointerItem.Mode.valueOf(tag.getString("Mode"));
        } catch (IllegalArgumentException e) {
            return LaserPointerItem.Mode.POINT;
        }
    }
    
    private static boolean hasPathStartPoint(ItemStack stack) {
        if (!stack.is(ModItems.LASER_POINTER.get())) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID("PathMarkerId");
    }
}