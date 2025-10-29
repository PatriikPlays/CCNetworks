package one.patriik.ccnetworks.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import one.patriik.ccnetworks.blockentity.NetworkInterfaceNodeBlockEntity;

import java.util.List;

public class NetworkInterfaceNodeBlockEntityRenderer implements BlockEntityRenderer<NetworkInterfaceNodeBlockEntity> {

    public NetworkInterfaceNodeBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(NetworkInterfaceNodeBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        List<BlockPos> links = blockEntity.getRenderLinks();
        BlockPos pos = blockEntity.getBlockPos();

        for (BlockPos link : links) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);

            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());

            float x1 = 0, y1 = 0, z1 = 0;
            float x2 = link.getX()-pos.getX(), y2 = link.getY()-pos.getY(), z2 = link.getZ()-pos.getZ();

            vertexConsumer.vertex(poseStack.last().pose(), x1, y1, z1)
                    .color(1f, 1f, 1f, 1f)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(packedLight)
                    .normal(poseStack.last().normal(), 0f, 1f, 0f)
                    .endVertex();

            vertexConsumer.vertex(poseStack.last().pose(), x2, y2, z2)
                    .color(1f, 1f, 1f, 1f)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(packedLight)
                    .normal(poseStack.last().normal(), 0f, 1f, 0f)
                    .endVertex();

            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(NetworkInterfaceNodeBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
