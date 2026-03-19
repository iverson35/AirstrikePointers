package dev.ignis.airstrikepointer;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_MARKERS_PER_PLAYER = BUILDER
            .comment("Maximum number of markers a player can create")
            .defineInRange("maxMarkersPerPlayer", 10, 1, 100);

    public static final ForgeConfigSpec.IntValue MARKER_LIFETIME_SECONDS = BUILDER
            .comment("Marker lifetime in seconds")
            .defineInRange("markerLifetimeSeconds", 30, 5, 300);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SHOW_ONLY_MY_TEAM = CLIENT_BUILDER
            .comment("Only show markers from your own team")
            .define("showOnlyMyTeam", false);

    public static final ForgeConfigSpec.BooleanValue SHOW_UNTEAM_MARKERS = CLIENT_BUILDER
            .comment("When team filtering is enabled, whether to show markers from players without a team")
            .define("showUnteamMarkers", true);

    static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
}
