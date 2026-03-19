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

    @SuppressWarnings("removal")
    public static final ModelResourceLocation LASER_POINTER_IN_HAND_MODEL =
        new ModelResourceLocation(new ResourceLocation(AirstrikePointers.MODID, "laser_pointer_in_hand"), "inventory");

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
        
        // 根据显示上下文选择模型
        ModelResourceLocation modelLocation;
        
        // 第一人称手持时使用特殊模型
        if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || 
            displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            modelLocation = LASER_POINTER_IN_HAND_MODEL;
        } else {
            modelLocation = LASER_POINTER_MODEL;
        }
        
        // 渲染模型
        itemRenderer.render(stack, displayContext, false, poseStack, buffer, packedLight, packedOverlay, 
            itemRenderer.getItemModelShaper().getModelManager().getModel(modelLocation));
    }
}