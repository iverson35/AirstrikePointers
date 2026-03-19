package dev.ignis.airstrikepointer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ignis.airstrikepointer.AirstrikePointers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LaserPointerItemRenderer extends BlockEntityWithoutLevelRenderer {

    @SuppressWarnings("removal")
    public static final ModelResourceLocation LASER_POINTER_MODEL = 
        new ModelResourceLocation(new ResourceLocation(AirstrikePointers.MODID, "laser_pointer"), "inventory");

    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final EntityModelSet entityModelSet;

    public LaserPointerItemRenderer(BlockEntityRenderDispatcher p_172550_, EntityModelSet p_172551_) {
        super(p_172550_, p_172551_);
        this.blockEntityRenderDispatcher = p_172550_;
        this.entityModelSet = p_172551_;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, 
                            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        
        // 使用单一模型
        itemRenderer.render(stack, displayContext, false, poseStack, buffer, packedLight, packedOverlay, 
            itemRenderer.getItemModelShaper().getModelManager().getModel(LASER_POINTER_MODEL));
    }
}