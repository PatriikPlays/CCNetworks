package one.patriik.ccnetworks.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;
import one.patriik.ccnetworks.client.CCNetworksClient;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

// TODO: optimize, cache stuff
// this is yet again vibecoded, buuut it doesn't seem to do anything very stupid
public class AbstractNetworkNodeBlockEntityRenderer<T extends AbstractNetworkNodeBlockEntity> implements BlockEntityRenderer<T> {
    private static final float CABLE_WIDTH = 0.05f;
    private static final float CONNECTION_OFFSET = 0.315f - 0.5f;
    private static final float MIN_CABLE_LENGTH = 0.01f;
    private static final int CABLE_TINT = 128;
    private static final ResourceLocation WOOL_TEXTURE = new ResourceLocation("minecraft", "block/black_wool");

    public AbstractNetworkNodeBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        List<BlockPos> links = blockEntity.getRenderLinks();
        if (level == null || links.isEmpty()) {
            return;
        }

        BlockPos blockPos = blockEntity.getBlockPos();
        Vector3f startOffset = getConnectionOffset(blockEntity.getBlockState());
        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        TextureAtlasSprite woolTexture = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(WOOL_TEXTURE);

        for (BlockPos link : links) {
            Vector3f targetOffset = getConnectionOffset(level.getBlockState(link));
            float targetX = link.getX() - blockPos.getX() + targetOffset.x() - startOffset.x();
            float targetY = link.getY() - blockPos.getY() + targetOffset.y() - startOffset.y();
            float targetZ = link.getZ() - blockPos.getZ() + targetOffset.z() - startOffset.z();
            double lengthSquared = (double) targetX * targetX + (double) targetY * targetY + (double) targetZ * targetZ;

            if (lengthSquared <= MIN_CABLE_LENGTH * MIN_CABLE_LENGTH || !Double.isFinite(lengthSquared)) {
                continue;
            }
            float length = (float) Math.sqrt(lengthSquared);
            float maxCableHalfLength = CCNetworksClient.CONFIG.maxCableHalfLength;
            if (maxCableHalfLength <= 0.0f) {
                continue;
            }
            float endT = Math.min(0.5f, maxCableHalfLength / length);
            float droop = (length * length / 4096.0f) * 2.0f;

            poseStack.pushPose();
            poseStack.translate(0.5 + startOffset.x(), 0.5 + startOffset.y(), 0.5 + startOffset.z());
            renderHalfCable(
                    poseStack.last(),
                    consumer,
                    targetX,
                    targetY,
                    targetZ,
                    endT,
                    droop,
                    woolTexture,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY
            );
            poseStack.popPose();
        }
    }

    private static float[] createSegmentParameters(
            Vector3f end,
            float endT,
            float droop,
            float targetSegmentLength,
            int maxSegmentsPerHalf
    ) {
        double arcLength = getArcLength(end, endT, droop);
        int segmentCount = getSegmentCount(arcLength, targetSegmentLength, maxSegmentsPerHalf);
        float[] parameters = new float[segmentCount + 1];

        for (int i = 0; i <= segmentCount; i++) {
            if (i == 0) {
                parameters[i] = 0.0f;
            } else if (i == segmentCount) {
                parameters[i] = endT;
            } else {
                parameters[i] = getParameterAtArcLength(end, endT, droop, arcLength * i / segmentCount, arcLength);
            }
        }

        return parameters;
    }

    private static double getArcLength(Vector3f end, double t, double droop) {
        double horizontalLength = Math.hypot(end.x(), end.z());
        double initialSlope = end.y() - 4.0 * droop;
        double slopeChange = 8.0 * droop * t;

        if (droop == 0.0) {
            return Math.hypot(horizontalLength, end.y()) * t;
        }

        double finalSlope = initialSlope + slopeChange;
        return Math.max(0.0, (
                arcLengthPrimitive(finalSlope, horizontalLength)
                        - arcLengthPrimitive(initialSlope, horizontalLength)
        ) / (8.0 * droop));
    }

    private static double arcLengthPrimitive(double slope, double horizontalLength) {
        if (horizontalLength == 0.0) {
            return 0.5 * slope * Math.abs(slope);
        }

        double ratio = slope / horizontalLength;
        double absoluteRatio = Math.abs(ratio);
        double inverseHyperbolicSine = Math.copySign(
                Math.log1p(absoluteRatio + absoluteRatio * absoluteRatio / (Math.hypot(absoluteRatio, 1.0) + 1.0)),
                ratio
        );
        return 0.5 * (
                slope * Math.hypot(horizontalLength, slope)
                        + horizontalLength * horizontalLength * inverseHyperbolicSine
        );
    }

    private static float getParameterAtArcLength(
            Vector3f end,
            double endT,
            double droop,
            double targetLength,
            double totalLength
    ) {
        double low = 0.0;
        double high = endT;
        double t = endT * targetLength / totalLength;
        double horizontalLength = Math.hypot(end.x(), end.z());

        for (int i = 0; i < 16; i++) {
            double currentLength = getArcLength(end, t, droop);
            double error = currentLength - targetLength;
            if (Math.abs(error) < 1.0e-5) {
                break;
            }

            if (error < 0.0) {
                low = t;
            } else {
                high = t;
            }

            double slope = end.y() - 4.0 * droop + 8.0 * droop * t;
            double speed = Math.hypot(horizontalLength, slope);
            double nextT = speed > 0.0 ? t - error / speed : Double.NaN;
            if (!Double.isFinite(nextT) || nextT <= low || nextT >= high) {
                nextT = (low + high) * 0.5;
            }
            if (nextT == t) {
                break;
            }
            t = nextT;
        }

        return (float) Math.max(0.0, Math.min(endT, t));
    }

    private static int getSegmentCount(double arcLength, float targetSegmentLength, int maxSegmentsPerHalf) {
        int lowerCount = Math.max(1, Math.min(
                maxSegmentsPerHalf,
                (int) Math.floor(arcLength / targetSegmentLength)
        ));
        int upperCount = Math.min(maxSegmentsPerHalf, lowerCount + 1);
        double lowerLength = arcLength / lowerCount;
        double upperLength = arcLength / upperCount;

        return Math.abs(lowerLength - targetSegmentLength) <= Math.abs(upperLength - targetSegmentLength)
                ? lowerCount
                : upperCount;
    }

    private Vector3f getConnectionOffset(BlockState state) {
        Direction facing = Direction.UP;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        return new Vector3f(
                facing.getStepX() * CONNECTION_OFFSET,
                facing.getStepY() * CONNECTION_OFFSET,
                facing.getStepZ() * CONNECTION_OFFSET
        );
    }

    private void renderHalfCable(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float targetX,
            float targetY,
            float targetZ,
            float endT,
            float droop,
            TextureAtlasSprite texture,
            int light,
            int overlay
    ) {
        Matrix4f positionMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        Vector3f end = new Vector3f(targetX, targetY, targetZ);
        Vector3f point = new Vector3f();
        Vector3f nextPoint = new Vector3f();
        Vector3f previousPoint = new Vector3f();
        Vector3f forward = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f up = new Vector3f();
        Vector3f edgeA = new Vector3f();
        Vector3f edgeB = new Vector3f();
        Vector3f normal = new Vector3f();
        float[] parameters = createSegmentParameters(
                end,
                endT,
                droop,
                CCNetworksClient.CONFIG.targetCableSegmentLength,
                CCNetworksClient.CONFIG.maxSegmentsPerHalf
        );
        int segmentCount = parameters.length - 1;
        Vector3f[] previousCorners = createCorners();
        Vector3f[] currentCorners = createCorners();
        float halfWidth = CABLE_WIDTH * 0.5f;

        for (int i = 0; i <= segmentCount; i++) {
            float t = parameters[i];
            getPointOnCable(end, t, droop, point);

            if (i == 0) {
                getPointOnCable(end, parameters[i + 1], droop, nextPoint);
                forward.set(nextPoint).sub(point);
            } else if (i == segmentCount) {
                forward.set(point).sub(previousPoint);
            } else {
                getPointOnCable(end, parameters[i + 1], droop, nextPoint);
                forward.set(nextPoint).sub(previousPoint);
            }

            if (forward.lengthSquared() < 1.0e-8f) {
                forward.set(end);
            }
            forward.normalize();
            fillCorners(point, forward, right, up, halfWidth, currentCorners);

            if (i == 0) {
                normal.set(forward).negate();
                addVertex(consumer, positionMatrix, normalMatrix, currentCorners[3], normal, texture, 0.0f, 16.0f, light, overlay);
                addVertex(consumer, positionMatrix, normalMatrix, currentCorners[2], normal, texture, 16.0f, 16.0f, light, overlay);
                addVertex(consumer, positionMatrix, normalMatrix, currentCorners[1], normal, texture, 16.0f, 0.0f, light, overlay);
                addVertex(consumer, positionMatrix, normalMatrix, currentCorners[0], normal, texture, 0.0f, 0.0f, light, overlay);
            } else {
                for (int side = 0; side < 4; side++) {
                    int nextSide = (side + 1) % 4;
                    Vector3f first = previousCorners[side];
                    Vector3f second = previousCorners[nextSide];
                    Vector3f fourth = currentCorners[side];

                    edgeA.set(second).sub(first);
                    edgeB.set(fourth).sub(first);
                    normal.set(edgeA).cross(edgeB).normalize();

                    addVertex(consumer, positionMatrix, normalMatrix, first, normal, texture, 0.0f, 0.0f, light, overlay);
                    addVertex(consumer, positionMatrix, normalMatrix, second, normal, texture, 16.0f, 0.0f, light, overlay);
                    addVertex(consumer, positionMatrix, normalMatrix, currentCorners[nextSide], normal, texture, 16.0f, 16.0f, light, overlay);
                    addVertex(consumer, positionMatrix, normalMatrix, fourth, normal, texture, 0.0f, 16.0f, light, overlay);
                }
            }

            Vector3f[] swap = previousCorners;
            previousCorners = currentCorners;
            currentCorners = swap;
            previousPoint.set(point);
        }
    }

    private static Vector3f[] createCorners() {
        return new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    }

    private static void fillCorners(
            Vector3f point,
            Vector3f forward,
            Vector3f right,
            Vector3f up,
            float halfWidth,
            Vector3f[] corners
    ) {
        if (Math.abs(forward.y()) > 0.99f) {
            right.set(forward).cross(1.0f, 0.0f, 0.0f);
        } else {
            right.set(forward).cross(0.0f, 1.0f, 0.0f);
        }
        right.normalize();
        up.set(right).cross(forward).normalize();

        corners[0].set(point).add(
                (right.x() + up.x()) * halfWidth,
                (right.y() + up.y()) * halfWidth,
                (right.z() + up.z()) * halfWidth
        );
        corners[1].set(point).add(
                (right.x() - up.x()) * halfWidth,
                (right.y() - up.y()) * halfWidth,
                (right.z() - up.z()) * halfWidth
        );
        corners[2].set(point).add(
                (-right.x() - up.x()) * halfWidth,
                (-right.y() - up.y()) * halfWidth,
                (-right.z() - up.z()) * halfWidth
        );
        corners[3].set(point).add(
                (-right.x() + up.x()) * halfWidth,
                (-right.y() + up.y()) * halfWidth,
                (-right.z() + up.z()) * halfWidth
        );
    }

    private static void getPointOnCable(Vector3f end, float t, float droop, Vector3f result) {
        float sag = droop * 4.0f * t * (1.0f - t);
        result.set(end.x() * t, end.y() * t - sag, end.z() * t);
    }

    private static void addVertex(
            VertexConsumer consumer,
            Matrix4f positionMatrix,
            Matrix3f normalMatrix,
            Vector3f position,
            Vector3f normal,
            TextureAtlasSprite texture,
            float u,
            float v,
            int light,
            int overlay
    ) {
        consumer.vertex(positionMatrix, position.x(), position.y(), position.z())
                .color(CABLE_TINT, CABLE_TINT, CABLE_TINT, 255)
                .uv(texture.getU(u), texture.getV(v))
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
        return Mth.ceil(CCNetworksClient.CONFIG.maxNodeRenderDistance);
    }
}
