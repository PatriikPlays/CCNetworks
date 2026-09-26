package one.patriik.ccnetworks.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import one.patriik.ccnetworks.CCNetworks;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;
import one.patriik.ccnetworks.client.CCNetworksClient;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CableRenderer {
    private static final float CABLE_WIDTH = 0.05f;
    private static final float CONNECTION_OFFSET = 0.315f - 0.5f;
    private static final float MIN_CABLE_LENGTH = 0.01f;
    private static final int CABLE_TINT = 128;
    private static final ResourceLocation WOOL_TEXTURE = new ResourceLocation("minecraft", "block/black_wool");

    private final Map<BlockPos, AbstractNetworkNodeBlockEntity> nodes = new HashMap<>();
    private final Map<BlockPos, Map<CableKey, CableGeometry>> cachedGeometry = new HashMap<>();
    private ClientLevel world;
    private GeometrySettings geometrySettings;

    public static void initialize() {
        CableRenderer renderer = new CableRenderer();
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register(renderer::addNode);
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(renderer::removeNode);
        WorldRenderEvents.AFTER_ENTITIES.register(renderer::render);
    }

    private void addNode(BlockEntity blockEntity, ClientLevel eventWorld) {
        if (blockEntity instanceof AbstractNetworkNodeBlockEntity node) {
            setWorld(eventWorld);
            nodes.put(node.getBlockPos().immutable(), node);
        }
    }

    private void removeNode(BlockEntity blockEntity, ClientLevel eventWorld) {
        if (eventWorld == world && blockEntity instanceof AbstractNetworkNodeBlockEntity node) {
            BlockPos position = node.getBlockPos();
            if (nodes.remove(position, node)) {
                cachedGeometry.remove(position);
            }
        }
    }

    private void setWorld(ClientLevel newWorld) {
        if (world != newWorld) {
            nodes.clear();
            cachedGeometry.clear();
            world = newWorld;
        }
    }

    private void render(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        renderFrame(context);
    }

    private void renderFrame(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        ClientLevel currentWorld = context.world();
        setWorld(currentWorld);

        MultiBufferSource consumers = context.consumers();
        if (consumers == null || nodes.isEmpty()) {
            return;
        }

        GeometrySettings currentSettings = GeometrySettings.fromConfig();
        if (!currentSettings.equals(geometrySettings)) {
            cachedGeometry.clear();
            geometrySettings = currentSettings;
        }
        if (currentSettings.maxCableHalfLength <= 0.0f) {
            return;
        }

        Vec3 cameraPosition = context.camera().getPosition();
        double maxRenderDistance = CCNetworksClient.CONFIG.maxNodeRenderDistance;
        double maxRenderDistanceSquared = maxRenderDistance * maxRenderDistance;
        VertexConsumer consumer = consumers.getBuffer(RenderType.solid());
        TextureAtlasSprite woolTexture = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(WOOL_TEXTURE);
        PoseStack.Pose pose = context.matrixStack().last();

        for (var iterator = nodes.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<BlockPos, AbstractNetworkNodeBlockEntity> entry = iterator.next();
            BlockPos source = entry.getKey();
            AbstractNetworkNodeBlockEntity node = entry.getValue();
            if (currentWorld.getBlockEntity(source) != node) {
                iterator.remove();
                cachedGeometry.remove(source);
                continue;
            }

            if (source.distToCenterSqr(cameraPosition) > maxRenderDistanceSquared) {
                continue;
            }

            renderNodeLinks(currentWorld, source, node, currentSettings, cameraPosition, consumer, woolTexture, pose);
        }
    }

    private void renderNodeLinks(
            ClientLevel level,
            BlockPos source,
            AbstractNetworkNodeBlockEntity node,
            GeometrySettings settings,
            Vec3 cameraPosition,
            VertexConsumer consumer,
            TextureAtlasSprite texture,
            PoseStack.Pose pose
    ) {
        Vector3f sourceOffset = getConnectionOffset(node.getBlockState());
        Map<CableKey, CableGeometry> nodeGeometry = cachedGeometry.computeIfAbsent(source, ignored -> new HashMap<>());
        Set<CableKey> currentLinks = new HashSet<>();

        for (BlockPos target : node.getRenderLinks()) {
            CableKey key = CableKey.create(target, node.getBlockState(), level.getBlockState(target));
            currentLinks.add(key);
            CableGeometry geometry = nodeGeometry.computeIfAbsent(
                    key,
                    ignored -> createGeometry(source, target, sourceOffset, getConnectionOffset(level.getBlockState(target)), settings)
            );
            if (geometry != null) {
                geometry.render(consumer, cameraPosition, texture, pose);
            }
        }

        nodeGeometry.keySet().retainAll(currentLinks);
    }

    private static CableGeometry createGeometry(
            BlockPos source,
            BlockPos target,
            Vector3f sourceOffset,
            Vector3f targetOffset,
            GeometrySettings settings
    ) {
        Vector3f end = new Vector3f(
                target.getX() - source.getX() + targetOffset.x() - sourceOffset.x(),
                target.getY() - source.getY() + targetOffset.y() - sourceOffset.y(),
                target.getZ() - source.getZ() + targetOffset.z() - sourceOffset.z()
        );
        double lengthSquared = end.lengthSquared();
        if (lengthSquared <= MIN_CABLE_LENGTH * MIN_CABLE_LENGTH || !Double.isFinite(lengthSquared) || settings.maxCableHalfLength <= 0.0f) {
            return null;
        }

        float length = (float) Math.sqrt(lengthSquared);
        float endT = Math.min(0.5f, settings.maxCableHalfLength / length);
        float droop = (length * length / 4096.0f) * 2.0f;
        float[] parameters = createSegmentParameters(end, endT, droop, settings.targetSegmentLength, settings.maxSegmentsPerHalf);
        GeometryBuilder builder = new GeometryBuilder(parameters.length * 64);
        Vector3f point = new Vector3f();
        Vector3f nextPoint = new Vector3f();
        Vector3f previousPoint = new Vector3f();
        Vector3f forward = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f up = new Vector3f();
        Vector3f edgeA = new Vector3f();
        Vector3f edgeB = new Vector3f();
        Vector3f normal = new Vector3f();
        Vector3f[] previousCorners = createCorners();
        Vector3f[] currentCorners = createCorners();
        float halfWidth = CABLE_WIDTH * 0.5f;

        for (int i = 0; i < parameters.length; i++) {
            float t = parameters[i];
            getPointOnCable(end, t, droop, point);

            if (i == 0) {
                getPointOnCable(end, parameters[i + 1], droop, nextPoint);
                forward.set(nextPoint).sub(point);
            } else if (i == parameters.length - 1) {
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
                builder.add(currentCorners[3], normal, 0.0f, 1.0f);
                builder.add(currentCorners[2], normal, 1.0f, 1.0f);
                builder.add(currentCorners[1], normal, 1.0f, 0.0f);
                builder.add(currentCorners[0], normal, 0.0f, 0.0f);
            } else {
                for (int side = 0; side < 4; side++) {
                    int nextSide = (side + 1) % 4;
                    Vector3f first = previousCorners[side];
                    Vector3f second = previousCorners[nextSide];
                    Vector3f fourth = currentCorners[side];

                    edgeA.set(second).sub(first);
                    edgeB.set(fourth).sub(first);
                    normal.set(edgeA).cross(edgeB).normalize();

                    builder.add(first, normal, 0.0f, 0.0f);
                    builder.add(second, normal, 1.0f, 0.0f);
                    builder.add(currentCorners[nextSide], normal, 1.0f, 1.0f);
                    builder.add(fourth, normal, 0.0f, 1.0f);
                }
            }

            Vector3f[] swap = previousCorners;
            previousCorners = currentCorners;
            currentCorners = swap;
            previousPoint.set(point);
        }

        return new CableGeometry(
                source.getX() + 0.5 + sourceOffset.x(),
                source.getY() + 0.5 + sourceOffset.y(),
                source.getZ() + 0.5 + sourceOffset.z(),
                builder.build()
        );
    }

    private static float[] createSegmentParameters(Vector3f end, float endT, float droop, float targetSegmentLength, int maxSegmentsPerHalf) {
        double arcLength = getArcLength(end, endT, droop);
        int segmentCount = getSegmentCount(arcLength, targetSegmentLength, maxSegmentsPerHalf);
        float[] parameters = new float[segmentCount + 1];

        for (int i = 0; i <= segmentCount; i++) {
            parameters[i] = i == 0 ? 0.0f : i == segmentCount
                    ? endT
                    : getParameterAtArcLength(end, endT, droop, arcLength * i / segmentCount, arcLength);
        }
        return parameters;
    }

    private static double getArcLength(Vector3f end, double t, double droop) {
        double horizontalLength = Math.hypot(end.x(), end.z());
        double initialSlope = end.y() - 4.0 * droop;
        if (droop == 0.0) {
            return Math.hypot(horizontalLength, end.y()) * t;
        }

        double finalSlope = initialSlope + 8.0 * droop * t;
        return Math.max(0.0, (arcLengthPrimitive(finalSlope, horizontalLength) - arcLengthPrimitive(initialSlope, horizontalLength)) / (8.0 * droop));
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
        return 0.5 * (slope * Math.hypot(horizontalLength, slope) + horizontalLength * horizontalLength * inverseHyperbolicSine);
    }

    private static float getParameterAtArcLength(Vector3f end, double endT, double droop, double targetLength, double totalLength) {
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
        int lowerCount = Math.max(1, Math.min(maxSegmentsPerHalf, (int) Math.floor(arcLength / targetSegmentLength)));
        int upperCount = Math.min(maxSegmentsPerHalf, lowerCount + 1);
        return Math.abs(arcLength / lowerCount - targetSegmentLength) <= Math.abs(arcLength / upperCount - targetSegmentLength)
                ? lowerCount
                : upperCount;
    }

    private static Vector3f getConnectionOffset(BlockState state) {
        Direction facing = Direction.UP;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return new Vector3f(facing.getStepX() * CONNECTION_OFFSET, facing.getStepY() * CONNECTION_OFFSET, facing.getStepZ() * CONNECTION_OFFSET);
    }

    private static Vector3f[] createCorners() {
        return new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    }

    private static void fillCorners(Vector3f point, Vector3f forward, Vector3f right, Vector3f up, float halfWidth, Vector3f[] corners) {
        if (Math.abs(forward.y()) > 0.99f) {
            right.set(forward).cross(1.0f, 0.0f, 0.0f);
        } else {
            right.set(forward).cross(0.0f, 1.0f, 0.0f);
        }
        right.normalize();
        up.set(right).cross(forward).normalize();

        corners[0].set(point).add((right.x() + up.x()) * halfWidth, (right.y() + up.y()) * halfWidth, (right.z() + up.z()) * halfWidth);
        corners[1].set(point).add((right.x() - up.x()) * halfWidth, (right.y() - up.y()) * halfWidth, (right.z() - up.z()) * halfWidth);
        corners[2].set(point).add((-right.x() - up.x()) * halfWidth, (-right.y() - up.y()) * halfWidth, (-right.z() - up.z()) * halfWidth);
        corners[3].set(point).add((-right.x() + up.x()) * halfWidth, (-right.y() + up.y()) * halfWidth, (-right.z() + up.z()) * halfWidth);
    }

    private static void getPointOnCable(Vector3f end, float t, float droop, Vector3f result) {
        float sag = droop * 4.0f * t * (1.0f - t);
        result.set(end.x() * t, end.y() * t - sag, end.z() * t);
    }

    private record CableKey(BlockPos target, Direction sourceFacing, Direction targetFacing) {
        private static CableKey create(BlockPos target, BlockState sourceState, BlockState targetState) {
            return new CableKey(target.immutable(), getFacing(sourceState), getFacing(targetState));
        }
    }

    private record GeometrySettings(float maxCableHalfLength, float targetSegmentLength, int maxSegmentsPerHalf) {
        private static GeometrySettings fromConfig() {
            return new GeometrySettings(
                    CCNetworksClient.CONFIG.maxCableHalfLength,
                    CCNetworksClient.CONFIG.targetCableSegmentLength,
                    CCNetworksClient.CONFIG.maxSegmentsPerHalf
            );
        }
    }

    private record CableGeometry(double originX, double originY, double originZ, float[] vertices) {
        private void render(VertexConsumer consumer, Vec3 cameraPosition, TextureAtlasSprite texture, PoseStack.Pose pose) {
            for (int i = 0; i < vertices.length; i += 8) {
                consumer.vertex(
                                pose.pose(),
                                (float) (originX + vertices[i] - cameraPosition.x),
                                (float) (originY + vertices[i + 1] - cameraPosition.y),
                                (float) (originZ + vertices[i + 2] - cameraPosition.z)
                        )
                        .color(CABLE_TINT, CABLE_TINT, CABLE_TINT, 255)
                        .uv(texture.getU(vertices[i + 6]), texture.getV(vertices[i + 7]))
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(LightTexture.FULL_BRIGHT)
                        .normal(pose.normal(), vertices[i + 3], vertices[i + 4], vertices[i + 5])
                        .endVertex();
            }
        }
    }

    private static final class GeometryBuilder {
        private float[] values;
        private int size;

        private GeometryBuilder(int expectedVertexValues) {
            values = new float[expectedVertexValues];
        }

        private void add(Vector3f position, Vector3f normal, float u, float v) {
            ensureCapacity(8);
            values[size++] = position.x();
            values[size++] = position.y();
            values[size++] = position.z();
            values[size++] = normal.x();
            values[size++] = normal.y();
            values[size++] = normal.z();
            values[size++] = u;
            values[size++] = v;
        }

        private float[] build() {
            return Arrays.copyOf(values, size);
        }

        private void ensureCapacity(int additionalValues) {
            int requiredCapacity = size + additionalValues;
            if (requiredCapacity > values.length) {
                values = Arrays.copyOf(values, Math.max(requiredCapacity, values.length * 2));
            }
        }
    }

    private static Direction getFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.UP;
    }
}
