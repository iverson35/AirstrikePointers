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

    // Guidance System Config
    public static final ForgeConfigSpec.BooleanValue GUIDANCE_ENABLED = BUILDER
            .comment("Enable guidance system for tracked entities")
            .define("guidance.enabled", false);

    public static final ForgeConfigSpec.IntValue GUIDANCE_HORIZONTAL_RANGE = BUILDER
            .comment("Horizontal range for guidance search (blocks)")
            .defineInRange("guidance.horizontalRange", 32, 1, 128);

    public static final ForgeConfigSpec.IntValue GUIDANCE_VERTICAL_RANGE = BUILDER
            .comment("Vertical range for guidance search (blocks)")
            .defineInRange("guidance.verticalRange", 64, 1, 256);

    public static final ForgeConfigSpec.IntValue GUIDANCE_VERTICAL_OFFSET = BUILDER
            .comment("Vertical offset from tracked entity for guidance center (blocks)")
            .defineInRange("guidance.verticalOffset", 32, -128, 128);

    public static final ForgeConfigSpec.ConfigValue<java.util.List<String>> GUIDANCE_ENTITY_LIST = BUILDER
            .comment("List of entity IDs that can be guided (e.g., [\"minecraft:arrow\", \"minecraft:snowball\"])")
            .define("guidance.entityList", java.util.Arrays.asList("minecraft:arrow", "minecraft:snowball", "minecraft:egg", "minecraft:trident"));

    public static final ForgeConfigSpec.IntValue GUIDANCE_INTERVAL = BUILDER
            .comment("Guidance interval in ticks (1 = every tick)")
            .defineInRange("guidance.interval", 1, 1, 100);

    public static final ForgeConfigSpec.DoubleValue GUIDANCE_RATIO = BUILDER
            .comment("Guidance ratio (0.0-1.0), how much velocity is redirected toward target")
            .defineInRange("guidance.ratio", 0.1, 0.0, 1.0);

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
