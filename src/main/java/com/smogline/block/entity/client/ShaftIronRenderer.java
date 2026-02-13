package com.smogline.block.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.smogline.block.custom.rotation.ShaftIronBlock;
import com.smogline.block.entity.custom.ShaftIronBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ShaftIronRenderer extends GeoBlockRenderer<ShaftIronBlockEntity> {

    public ShaftIronRenderer(BlockEntityRendererProvider.Context context) {
        super(new ShaftIronModel());
    }

    // GeckoLib 4.x использует preRender для трансформаций
    @Override
    public void preRender(PoseStack poseStack, ShaftIronBlockEntity animatable,
                          BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, float red, float green,
                          float blue, float alpha) {

        BlockState state = animatable.getBlockState();
        Direction facing = state.getValue(ShaftIronBlock.FACING);

        // Смещаем к центру
        poseStack.translate(0.5f, 0.5f, 0.5f);

        // Поворот только для идентификации направления
        switch (facing) {
            case NORTH:
                break;
            case SOUTH:
                break;
            case EAST:
                break;
            case WEST:
                break;
            case UP:
                poseStack.mulPose(Axis.XP.rotationDegrees(45));
                break;
            case DOWN:
                poseStack.mulPose(Axis.XP.rotationDegrees(-45));
                break;
        }

        poseStack.translate(-0.5f, -0.5f, -0.5f);

        super.preRender(poseStack, animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);
    }
}