package dev.ignis.airstrikepointer;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_MARKERS_PER_PLAYER = BUILDER
            .comment("Maximum number of markers a player can create")
            .defineInRange("maxMarkersPerPlayer", 10, 1, 100);

    public static final ForgeConfigSpec.IntValue MARKER_LIFETIME_SECONDS = BUILDER
            .comment("Marker lifetime in seconds")
            .defineInRange("markerLifetimeSeconds", 30, 5, 300);

    public static final ForgeConfigSpec.IntValue MARKER_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown between marker uses in ticks (20 ticks = 1 second, 0 = no cooldown)")
            .defineInRange("markerCooldownTicks", 100, 0, 600);

    // Guidance System Config
    public static final ForgeConfigSpec.BooleanValue GUIDANCE_ENABLED = BUILDER
            .comment("Enable guidance system for tracked entities")
            .define("guidance.enabled", true);

    public static final ForgeConfigSpec.IntValue GUIDANCE_HORIZONTAL_RANGE = BUILDER
            .comment("Horizontal range for guidance search (blocks)")
            .defineInRange("guidance.horizontalRange", 32, 1, 128);

    public static final ForgeConfigSpec.IntValue GUIDANCE_VERTICAL_RANGE = BUILDER
            .comment("Vertical range for guidance search (blocks)")
            .defineInRange("guidance.verticalRange", 64, 1, 256);

    public static final ForgeConfigSpec.IntValue GUIDANCE_VERTICAL_OFFSET = BUILDER
            .comment("Vertical offset from tracked entity for guidance center (blocks)")
            .defineInRange("guidance.verticalOffset", 32, -128, 128);

    public static final ForgeConfigSpec.ConfigValue<List<String>> GUIDANCE_ENTITY_LIST = BUILDER
            .comment("List of entity IDs that can be guided (e.g., [\"minecraft:arrow\", \"minecraft:snowball\"])")
            .define("guidance.entityList", new java.util.ArrayList<>(java.util.List.of("minecraft:arrow")));

    public static final ForgeConfigSpec.IntValue GUIDANCE_INTERVAL = BUILDER
            .comment("Guidance interval in ticks (1 = every tick)")
            .defineInRange("guidance.interval", 1, 1, 100);

    public static final ForgeConfigSpec.DoubleValue GUIDANCE_RATIO = BUILDER
            .comment("Guidance ratio (0.0-1.0), how much velocity is redirected toward target")
            .defineInRange("guidance.ratio", 0.2, 0.0, 1.0);

    public static final ForgeConfigSpec.BooleanValue GUIDANCE_GUIDE_CBC_SHELLS = BUILDER
            .comment("Guide all Create Big Cannons shells (AbstractBigCannonProjectile), regardless of config list")
            .define("guidance.guideCbcShells", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SHOW_ONLY_MY_TEAM = CLIENT_BUILDER
            .comment("Only show markers from your own team")
            .define("showOnlyMyTeam", false);

    public static final ForgeConfigSpec.BooleanValue SHOW_UNTEAM_MARKERS = CLIENT_BUILDER
            .comment("When team filtering is enabled, whether to show markers from players without a team")
            .define("showUnteamMarkers", true);

    public static final ForgeConfigSpec.IntValue MAX_RENDER_DISTANCE = CLIENT_BUILDER
            .comment("Maximum distance to render markers (blocks). 0 = unlimited")
            .defineInRange("maxRenderDistance", 0, 0, 10000);

    public static final ForgeConfigSpec.IntValue WHEEL_HOLD_THRESHOLD_TICKS = CLIENT_BUILDER
            .comment("How many ticks to hold right-click before the marker wheel appears (20 ticks = 1 second)")
            .defineInRange("wheelHoldThresholdTicks", 3, 1, 40);

    static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    // 配置重载回调
    private static Runnable onReload = () -> {};

    public static void setOnReload(Runnable callback) {
        onReload = callback;
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC || event.getConfig().getSpec() == CLIENT_SPEC) {
            onReload.run();
        }
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC || event.getConfig().getSpec() == CLIENT_SPEC) {
            onReload.run();
        }
    }
}
