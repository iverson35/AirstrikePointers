package dev.ignis.airstrikepointer.client;

import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LaserPointerSensitivityHandler {

    // 与 FOV 修改器一致的放大倍数
    public static final float ZOOM_FOV_MODIFIER = 0.3F;
    
    // 保存原始灵敏度
    private static double originalSensitivity = -1;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        // 只在第一人称时生效
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            // 非第一人称时恢复灵敏度
            if (originalSensitivity >= 0) {
                minecraft.options.sensitivity().set(originalSensitivity);
                originalSensitivity = -1;
            }
            return;
        }
        
        Options options = minecraft.options;
        
        // 检查是否正在使用激光指示器
        boolean isUsingLaserPointer = minecraft.player.isUsingItem() && 
                                       minecraft.player.getUseItem().is(ModItems.LASER_POINTER.get());
        
        if (isUsingLaserPointer) {
            // 保存原始灵敏度
            if (originalSensitivity < 0) {
                originalSensitivity = options.sensitivity().get();
            }
            
            // 计算缩放后的灵敏度：放大倍数越小，灵敏度越低
            // 使用 FOV 修改器的平方来匹配视角变化
            double sensitivityMultiplier = ZOOM_FOV_MODIFIER * ZOOM_FOV_MODIFIER;
            options.sensitivity().set(originalSensitivity * sensitivityMultiplier);
            
        } else {
            // 恢复原始灵敏度
            if (originalSensitivity >= 0) {
                options.sensitivity().set(originalSensitivity);
                originalSensitivity = -1;
            }
        }
    }
}