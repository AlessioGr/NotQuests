package com.notquests.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;

import com.notquests.core.structs.ActiveObjectives;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoForgeClientBeamRenderer {
    private static final int PRIMARY_COLOR = 0xE65EF6FF;
    private static final int SECONDARY_COLOR = 0xB0FFF06A;
    private static final Map<String, BeamLocation> BEAMS = new ConcurrentHashMap<>();

    private NeoForgeClientBeamRenderer() {}

    public static void register(final IEventBus modBus) {
        modBus.addListener(NeoForgeClientBeamRenderer::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientBeamRenderer::onSubmitCustomGeometry);
    }

    private static void registerPayloadHandlers(final RegisterClientPayloadHandlersEvent event) {
        event.register(NeoForgeBeamPayload.TYPE, NeoForgeClientBeamRenderer::handlePayload);
    }

    private static void handlePayload(final NeoForgeBeamPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.remove()) {
                BEAMS.remove(payload.beamName());
                return;
            }
            BEAMS.put(payload.beamName(), new BeamLocation(
                    payload.worldName(),
                    payload.x(),
                    payload.y(),
                    payload.z(),
                    payload.beaconMode()));
        });
    }

    private static void onSubmitCustomGeometry(final SubmitCustomGeometryEvent event) {
        if (BEAMS.isEmpty()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            BEAMS.clear();
            return;
        }
        final String currentWorld = level.dimension().identifier().toString();
        final String currentWorldPath = level.dimension().identifier().getPath();
        final String currentWorldDisplay = NeoForgeWorldNames.displayName(level.dimension().identifier());
        final Camera camera = minecraft.gameRenderer.getMainCamera();
        final Vec3 cameraPosition = camera.position();
        for (final BeamLocation beam : BEAMS.values()) {
            if (!beam.worldName().equalsIgnoreCase(currentWorld)
                    && !beam.worldName().equalsIgnoreCase(currentWorldPath)
                    && !beam.worldName().equalsIgnoreCase(currentWorldDisplay)) {
                continue;
            }
            submitBeam(event, beam, cameraPosition);
        }
    }

    private static void submitBeam(
            final SubmitCustomGeometryEvent event,
            final BeamLocation beam,
            final Vec3 cameraPosition) {
        final ActiveObjectives.ClientBeam marker = ActiveObjectives.clientBeam(
                new ActiveObjectives.BeamPoint(cameraPosition.x, cameraPosition.y, cameraPosition.z),
                new ActiveObjectives.BeamPoint(beam.x(), beam.y(), beam.z()));
        final PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        event.getSubmitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.linesTranslucent(),
                (pose, buffer) -> renderMarker(pose, buffer, marker, beam.beaconMode()));
        poseStack.popPose();
    }

    private static void renderMarker(
            final PoseStack.Pose pose,
            final VertexConsumer buffer,
            final ActiveObjectives.ClientBeam marker,
            final boolean beaconMode) {
        final float x = (float) marker.x();
        final float y = (float) marker.y();
        final float z = (float) marker.z();
        final float height = (float) marker.height();
        final float radius = beaconMode
                ? (marker.proxy() ? 1.25f : 0.75f)
                : (marker.proxy() ? 0.9f : 0.5f);
        final int primary = beaconMode
                ? (marker.proxy() ? SECONDARY_COLOR : PRIMARY_COLOR)
                : SECONDARY_COLOR;
        final int secondary = beaconMode
                ? (marker.proxy() ? PRIMARY_COLOR : SECONDARY_COLOR)
                : PRIMARY_COLOR;

        line(buffer, pose, x, y, z, x, y + height, z, primary, beaconMode ? 4.0f : 2.5f);
        line(buffer, pose, x - radius, y, z - radius, x + radius, y, z - radius, secondary, 2.5f);
        line(buffer, pose, x + radius, y, z - radius, x + radius, y, z + radius, secondary, 2.5f);
        line(buffer, pose, x + radius, y, z + radius, x - radius, y, z + radius, secondary, 2.5f);
        line(buffer, pose, x - radius, y, z + radius, x - radius, y, z - radius, secondary, 2.5f);
        line(buffer, pose, x - radius, y + 2.0f, z, x + radius, y + 2.0f, z, secondary, 2.5f);
        line(buffer, pose, x, y + 2.0f, z - radius, x, y + 2.0f, z + radius, secondary, 2.5f);
        line(buffer, pose, x - radius * 0.7f, y, z - radius * 0.7f, x, y + 2.0f, z, secondary, 2.5f);
        line(buffer, pose, x + radius * 0.7f, y, z - radius * 0.7f, x, y + 2.0f, z, secondary, 2.5f);
        line(buffer, pose, x + radius * 0.7f, y, z + radius * 0.7f, x, y + 2.0f, z, secondary, 2.5f);
        line(buffer, pose, x - radius * 0.7f, y, z + radius * 0.7f, x, y + 2.0f, z, secondary, 2.5f);
    }

    private static void line(
            final VertexConsumer buffer,
            final PoseStack.Pose pose,
            final float x1,
            final float y1,
            final float z1,
            final float x2,
            final float y2,
            final float z2,
            final int color,
            final float width) {
        final Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        if (normal.lengthSquared() == 0.0f) {
            normal.set(0.0f, 1.0f, 0.0f);
        } else {
            normal.normalize();
        }
        buffer.addVertex(pose, x1, y1, z1)
                .setColor(color)
                .setNormal(pose, normal.x(), normal.y(), normal.z())
                .setLineWidth(width);
        buffer.addVertex(pose, x2, y2, z2)
                .setColor(color)
                .setNormal(pose, normal.x(), normal.y(), normal.z())
                .setLineWidth(width);
    }

    private record BeamLocation(
            String worldName,
            double x,
            double y,
            double z,
            boolean beaconMode) {}
}
