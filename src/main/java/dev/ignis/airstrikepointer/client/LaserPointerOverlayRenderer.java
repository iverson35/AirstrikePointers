package dev.ignis.airstrikepointer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LaserPointerOverlayRenderer {

    // 可以创建一个自定义的覆盖层纹理
    // 暂时使用简单的十字准星作为示例
    
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // 只有当玩家使用激光指示器时才渲染
        if (minecraft.player == null || !minecraft.player.isUsingItem()) {
            return;
        }
        
        if (!minecraft.player.getUseItem().is(ModItems.LASER_POINTER.get())) {
            return;
        }
        
        // 渲染简单的十字准星覆盖层
        renderCrosshair(event.getGuiGraphics(), minecraft.getWindow().getGuiScaledWidth(), 
                       minecraft.getWindow().getGuiScaledHeight());
    }
    
    private static void renderCrosshair(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        // 绘制简单的十字准星
        RenderSystem.enableBlend();
        
        // 水平线
        guiGraphics.fill(centerX - 10, centerY - 1, centerX + 10, centerY + 1, 0xFF00FF00);
        // 垂直线
        guiGraphics.fill(centerX - 1, centerY - 10, centerX + 1, centerY + 10, 0xFF00FF00);
        
        // 中心点
        guiGraphics.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFFFF0000);
        
        RenderSystem.disableBlend();
    }
}