package dev.ignis.airstrikepointer;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_MARKERS_PER_PLAYER = BUILDER
            .comment("每个玩家最多可创建的标记数量")
            .defineInRange("maxMarkersPerPlayer", 10, 1, 100);

    public static final ForgeConfigSpec.IntValue MARKER_LIFETIME_SECONDS = BUILDER
            .comment("标记存活时间（秒）")
            .defineInRange("markerLifetimeSeconds", 30, 5, 300);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SHOW_ONLY_MY_TEAM = CLIENT_BUILDER
            .comment("只显示自己团队的标记")
            .define("showOnlyMyTeam", false);

    public static final ForgeConfigSpec.BooleanValue SHOW_UNTEAM_MARKERS = CLIENT_BUILDER
            .comment("当开启团队过滤时，是否显示无团队玩家的标记")
            .define("showUnteamMarkers", true);

    static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
}
