package dev.ignis.airstrikepointer.client;

import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LaserPointerFovHandler {

    // 放大倍数，1.0 = 无放大，0.1 = 10倍放大
    public static final float ZOOM_FOV_MODIFIER = 0.3F;

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // 只有当玩家使用激光指示器时才修改FOV
        if (minecraft.player == null || !minecraft.player.isUsingItem()) {
            return;
        }
        
        if (!minecraft.player.getUseItem().is(ModItems.LASER_POINTER.get())) {
            return;
        }
        
        // 设置FOV修改器实现放大效果
        event.setNewFovModifier(ZOOM_FOV_MODIFIER);
    }
}