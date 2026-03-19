package dev.ignis.airstrikepointer.client;

import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    // 放大倍数，1.0 = 无放大，0.1 = 10倍放大
    public static final float ZOOM_FOV_MODIFIER = 0.3F;

    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册望远镜使用动画的属性
            ItemProperties.register(
                ModItems.LASER_POINTER.get(),
                new ResourceLocation("using"),
                (stack, level, entity, seed) -> {
                    if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
                        return 1.0F;
                    }
                    return 0.0F;
                }
            );
        });
    }

    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // 注册激光指示器模型
        event.register(new ResourceLocation(AirstrikePointers.MODID, "item/laser_pointer"));
    }
}