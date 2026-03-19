package dev.ignis.airstrikepointer.client;

import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.items.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LaserPointerArmPoseHandler {

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        // 检查玩家是否正在使用激光指示器
        if (!player.isUsingItem() || !player.getUseItem().is(ModItems.LASER_POINTER.get())) return;
        
        // 获取模型
        if (event.getRenderer().getModel() instanceof HumanoidModel<?> model) {
            // 根据主手设置手臂姿势为 SPYGLASS
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                model.rightArmPose = HumanoidModel.ArmPose.SPYGLASS;
            } else {
                model.leftArmPose = HumanoidModel.ArmPose.SPYGLASS;
            }
        }
    }
}