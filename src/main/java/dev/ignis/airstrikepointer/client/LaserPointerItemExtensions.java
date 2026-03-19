package dev.ignis.airstrikepointer.client;

import dev.ignis.airstrikepointer.AirstrikePointers;
import dev.ignis.airstrikepointer.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class LaserPointerItemExtensions implements IClientItemExtensions {

    // 手持模型路径
    @SuppressWarnings("removal")
    public static final ModelResourceLocation LASER_POINTER_IN_HAND_MODEL = 
        new ModelResourceLocation(new ResourceLocation(AirstrikePointers.MODID, "laser_pointer_in_hand"), "inventory");

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return new LaserPointerItemRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), 
                                            Minecraft.getInstance().getEntityModels());
    }
}