package dev.ignis.airstrikepointer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LaserPointerOverlayRenderer {

    @SuppressWarnings("removal")
    // 使用自定义纹理
    private static final ResourceLocation SCOPE_LOCATION = new ResourceLocation(AirstrikePointers.MODID, "textures/gui/laser_pointer_scope.png");
    
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
        
        if (player == null || !player.isUsingItem()) return;
        
        if (!player.getUseItem().is(ModItems.LASER_POINTER.get())) return;
        
        // 渲染望远镜覆盖层
        renderSpyglassOverlay(event.getGuiGraphics());
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
        
        // 计算正方形区域
        float size = (float) Math.min(screenWidth, screenHeight);
        int renderSize = Mth.floor(size);
        int x = (screenWidth - renderSize) / 2;
        int y = (screenHeight - renderSize) / 2;
        
        // 渲染望远镜纹理（调整透明度以匹配原版效果）
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.1F); // 降低整体透明度
        gui.blit(SCOPE_LOCATION, x, y, -90, 0.0F, 0.0F, renderSize, renderSize, renderSize, renderSize);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // 恢复
        
        // 四角黑色遮罩
        int x2 = x + renderSize;
        int y2 = y + renderSize;
        gui.fill(RenderType.guiOverlay(), 0, y2, screenWidth, screenHeight, -90, -16777216);
        gui.fill(RenderType.guiOverlay(), 0, 0, screenWidth, y, -90, -16777216);
        gui.fill(RenderType.guiOverlay(), 0, y, x, y2, -90, -16777216);
        gui.fill(RenderType.guiOverlay(), x2, y, screenWidth, y2, -90, -16777216);
    }
}