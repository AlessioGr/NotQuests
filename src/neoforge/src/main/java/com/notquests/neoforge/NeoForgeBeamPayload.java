package com.notquests.neoforge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

record NeoForgeBeamPayload(
        String beamName,
        String worldName,
        double x,
        double y,
        double z,
        boolean beaconMode,
        boolean remove) implements CustomPacketPayload {
    static final CustomPacketPayload.Type<NeoForgeBeamPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NotQuestsNeoForge.MOD_ID, "beam_marker"));
    static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeBeamPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            NeoForgeBeamPayload::beamName,
            ByteBufCodecs.STRING_UTF8,
            NeoForgeBeamPayload::worldName,
            ByteBufCodecs.DOUBLE,
            NeoForgeBeamPayload::x,
            ByteBufCodecs.DOUBLE,
            NeoForgeBeamPayload::y,
            ByteBufCodecs.DOUBLE,
            NeoForgeBeamPayload::z,
            ByteBufCodecs.BOOL,
            NeoForgeBeamPayload::beaconMode,
            ByteBufCodecs.BOOL,
            NeoForgeBeamPayload::remove,
            NeoForgeBeamPayload::new);

    static NeoForgeBeamPayload show(final String beamName, final NeoForgeBeamTracker.BeamLocation location) {
        return new NeoForgeBeamPayload(
                beamName,
                location.worldName(),
                location.x(),
                location.y(),
                location.z(),
                location.beaconMode(),
                false);
    }

    static NeoForgeBeamPayload remove(final String beamName) {
        return new NeoForgeBeamPayload(beamName, "", 0.0d, 0.0d, 0.0d, false, true);
    }

    static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
