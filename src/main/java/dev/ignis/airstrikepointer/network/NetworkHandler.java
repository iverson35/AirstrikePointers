package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.AirstrikePointers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    @SuppressWarnings("removal")
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AirstrikePointers.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, CreatePointMarkerPacket.class,
                CreatePointMarkerPacket::encode, CreatePointMarkerPacket::decode, CreatePointMarkerPacket::handle);
        CHANNEL.registerMessage(packetId++, CreatePathMarkerPacket.class,
                CreatePathMarkerPacket::encode, CreatePathMarkerPacket::decode, CreatePathMarkerPacket::handle);
        CHANNEL.registerMessage(packetId++, ClearMarkersPacket.class,
                ClearMarkersPacket::encode, ClearMarkersPacket::decode, ClearMarkersPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncMarkersPacket.class,
                SyncMarkersPacket::encode, SyncMarkersPacket::decode, SyncMarkersPacket::handle);
        CHANNEL.registerMessage(packetId++, ModeSwitchPacket.class,
                ModeSwitchPacket::encode, ModeSwitchPacket::decode, ModeSwitchPacket::handle);
        CHANNEL.registerMessage(packetId++, RemoveMarkerPacket.class,
                RemoveMarkerPacket::encode, RemoveMarkerPacket::decode, RemoveMarkerPacket::handle);
    }
}
