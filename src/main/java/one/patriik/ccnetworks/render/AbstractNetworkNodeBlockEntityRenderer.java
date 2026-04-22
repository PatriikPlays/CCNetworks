package one.patriik.ccnetworks.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;

import java.util.List;

// this is mostly ai generated, ive looked through it and it seems sane though
public class AbstractNetworkNodeBlockEntityRenderer<T extends AbstractNetworkNodeBlockEntity> implements BlockEntityRenderer<T> {
    public AbstractNetworkNodeBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        List<BlockPos> links = blockEntity.getRenderLinks();
        BlockPos pos = blockEntity.getBlockPos();
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockState thisState = blockEntity.getBlockState();
        Vector3f startOffset = getConnectionOffset(thisState);

        for (BlockPos link : links) {
            BlockState targetState = level.getBlockState(link);
            Vector3f targetOffset = getConnectionOffset(targetState);

            // Calculate the relative coordinate of the target connection point from our connection point
            float x2 = (link.getX() - pos.getX()) + targetOffset.x() - startOffset.x();
            float y2 = (link.getY() - pos.getY()) + targetOffset.y() - startOffset.y();
            float z2 = (link.getZ() - pos.getZ()) + targetOffset.z() - startOffset.z();

            // Calculate exact straight-line distance
            float length = Mth.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
            if (length < 0.01f) continue; // Safety against zero-length cables

            poseStack.pushPose();

            // Translate to the exact starting connection point (center + offset)
            poseStack.translate(0.5 + startOffset.x(), 0.5 + startOffset.y(), 0.5 + startOffset.z());

            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());

            // Quadratic droop: 2m drop at 64 blocks distance (64^2 = 4096)
            float droop = ((length * length) / 4096.0f) * 2.0f;

            // Dynamically scale segments based on distance, minimum 4, always an even number
            int segments = Math.max(4, ((int) Math.ceil(length / 2.0f)) * 2);

            // Render cable half-way to target (with a 256 block render limit)
            renderHalfCable(poseStack, vertexConsumer, x2, y2, z2, length, 0.05f, droop, segments, packedLight, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }
    }

    /**
     * Calculates the offset from the center of the block (0.5, 0.5, 0.5)
     * to the top of the model (0.315 from the bottom side) based on block facing.
     */
    private Vector3f getConnectionOffset(BlockState state) {
        Direction facing = Direction.UP; // Default fallback

        if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        // If the bottom rests at 0.0, the top is at 0.315.
        // The distance from the center (0.5) is 0.315 - 0.5 = -0.185.
        float offsetDist = 0.315f - 0.5f;

        return new Vector3f(
                facing.getStepX() * offsetDist,
                facing.getStepY() * offsetDist,
                facing.getStepZ() * offsetDist
        );
    }

    private void renderHalfCable(PoseStack poseStack, VertexConsumer consumer, float targetX, float targetY, float targetZ, float length, float width, float droopAmount, int totalSegments, int light, int overlay) {
        float halfWidth = width / 2.0f;
        PoseStack.Pose entry = poseStack.last();
        Matrix4f positionMatrix = entry.pose();
        Matrix3f normalMatrix = entry.normal();

        Vector3f start = new Vector3f(0, 0, 0);
        Vector3f end = new Vector3f(targetX, targetY, targetZ);

        // We only iterate up to half the segments so we only draw the first 50% of the cable length
        int halfSegments = totalSegments / 2;

        // SAFETY CATCH: Render only up to 256 blocks away from this node
        int segmentsToDraw = halfSegments;
        if (length > 0) {
            int maxSafeSegments = (int) Math.ceil(totalSegments * (256.0f / length));
            segmentsToDraw = Math.min(halfSegments, maxSafeSegments);
        }

        Vector3f previousPoint = new Vector3f(start);
        Vector3f[] prevCorners = new Vector3f[4];

        for (int i = 0; i <= segmentsToDraw; i++) {
            float t = (float) i / totalSegments;
            Vector3f currentPoint = getPointOnCable(start, end, t, droopAmount);

            // Calculate the forward direction for normal generation
            Vector3f forward;
            if (i < totalSegments) {
                Vector3f nextPoint = getPointOnCable(start, end, t + 0.01f, droopAmount);
                forward = new Vector3f(nextPoint).sub(currentPoint).normalize();
            } else {
                forward = new Vector3f(currentPoint).sub(previousPoint).normalize();
            }

            // Calculate 'Right' and 'Up' vectors for the cross-section
            Vector3f globalUp = new Vector3f(0, 1, 0);
            if (Math.abs(forward.y()) > 0.99f) {
                globalUp = new Vector3f(1, 0, 0); // Handle perfectly vertical cables
            }

            Vector3f right = new Vector3f(forward).cross(globalUp).normalize();
            Vector3f up = new Vector3f(right).cross(forward).normalize();

            // 4 corners of the square cross-section around the center
            Vector3f rW = new Vector3f(right).mul(halfWidth);
            Vector3f uW = new Vector3f(up).mul(halfWidth);

            Vector3f[] currentCorners = new Vector3f[]{
                    new Vector3f(currentPoint).add(rW).add(uW),
                    new Vector3f(currentPoint).add(rW).sub(uW),
                    new Vector3f(currentPoint).sub(rW).sub(uW),
                    new Vector3f(currentPoint).sub(rW).add(uW)
            };

            if (i == 0) {
                // Cap off the start of the cable (t = 0) to hide the hollow inside
                Vector3f capNormal = new Vector3f(forward).mul(-1); // Facing backwards
                // Drawn in reverse order (3, 2, 1, 0) for correct backface culling
                addVertex(consumer, positionMatrix, normalMatrix, currentCorners[3], capNormal, light, overlay);
                addVertex(consumer, positionMatrix, normalMatrix, currentCorners[2], capNormal, light, overlay);
                addVertex(consumer, positionMatrix, normalMatrix, currentCorners[1], capNormal, light, overlay);
                addVertex(consumer, positionMatrix, normalMatrix, currentCorners[0], capNormal, light, overlay);
            } else {
                // Draw quads connecting the previous segment to the current segment
                for (int j = 0; j < 4; j++) {
                    int nextJ = (j + 1) % 4;

                    Vector3f p1 = prevCorners[j];
                    Vector3f p2 = prevCorners[nextJ];
                    Vector3f p3 = currentCorners[nextJ];
                    Vector3f p4 = currentCorners[j];

                    // Calculate face normal
                    Vector3f v1 = new Vector3f(p2).sub(p1);
                    Vector3f v2 = new Vector3f(p4).sub(p1);
                    Vector3f normal = v1.cross(v2).normalize();

                    addVertex(consumer, positionMatrix, normalMatrix, p1, normal, light, overlay);
                    addVertex(consumer, positionMatrix, normalMatrix, p2, normal, light, overlay);
                    addVertex(consumer, positionMatrix, normalMatrix, p3, normal, light, overlay);
                    addVertex(consumer, positionMatrix, normalMatrix, p4, normal, light, overlay);
                }
            }

            prevCorners = currentCorners;
            previousPoint = currentPoint;
        }
    }

    private Vector3f getPointOnCable(Vector3f start, Vector3f end, float t, float droopAmount) {
        float x = Mth.lerp(t, start.x(), end.x());
        float z = Mth.lerp(t, start.z(), end.z());
        float linearY = Mth.lerp(t, start.y(), end.y());

        // Parabolic droop equation: 0 at t=0 and t=1, max depth at t=0.5
        float droopOffset = droopAmount * 4 * t * (1 - t);

        return new Vector3f(x, linearY - droopOffset, z);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f posMatrix, Matrix3f normalMatrix, Vector3f pos, Vector3f normal, int light, int overlay) {
        consumer.vertex(posMatrix, pos.x(), pos.y(), pos.z())
                .color(80, 80, 80, 255)
                .uv(0, 0)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}