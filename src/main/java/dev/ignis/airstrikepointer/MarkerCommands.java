package dev.ignis.airstrikepointer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.ignis.airstrikepointer.markers.MarkerStorage;
import dev.ignis.airstrikepointer.markers.PointMarkerIcon;
import dev.ignis.airstrikepointer.network.CreatePointMarkerPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID)
public class MarkerCommands {

    private static final int[] MARKER_COLORS = {
            0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF,
            0x5555FF, 0xFF55FF, 0xFF8800, 0x00FF88, 0xFF0088,
            0x88FF00, 0x0088FF
    };

    private static final SuggestionProvider<CommandSourceStack> ICON_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(PointMarkerIcon.getIconIds(), builder);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("apmarker")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("create")
                    .then(Commands.argument("title", StringArgumentType.string())
                        .then(Commands.argument("description", StringArgumentType.string())
                            .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                    .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(context -> executeCreate(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "title"),
                                            StringArgumentType.getString(context, "description"),
                                            DoubleArgumentType.getDouble(context, "x"),
                                            DoubleArgumentType.getDouble(context, "y"),
                                            DoubleArgumentType.getDouble(context, "z"),
                                            null,
                                            null
                                        ))
                                        .then(Commands.argument("icon", StringArgumentType.word())
                                            .suggests(ICON_SUGGESTIONS)
                                            .executes(context -> executeCreate(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "title"),
                                                StringArgumentType.getString(context, "description"),
                                                DoubleArgumentType.getDouble(context, "x"),
                                                DoubleArgumentType.getDouble(context, "y"),
                                                DoubleArgumentType.getDouble(context, "z"),
                                                null,
                                                StringArgumentType.getString(context, "icon")
                                            ))
                                        )
                                        .then(Commands.argument("color", StringArgumentType.string())
                                            .executes(context -> executeCreate(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "title"),
                                                StringArgumentType.getString(context, "description"),
                                                DoubleArgumentType.getDouble(context, "x"),
                                                DoubleArgumentType.getDouble(context, "y"),
                                                DoubleArgumentType.getDouble(context, "z"),
                                                StringArgumentType.getString(context, "color"),
                                                null
                                            ))
                                            .then(Commands.argument("icon", StringArgumentType.word())
                                                .suggests(ICON_SUGGESTIONS)
                                                .executes(context -> executeCreate(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "title"),
                                                    StringArgumentType.getString(context, "description"),
                                                    DoubleArgumentType.getDouble(context, "x"),
                                                    DoubleArgumentType.getDouble(context, "y"),
                                                    DoubleArgumentType.getDouble(context, "z"),
                                                    StringArgumentType.getString(context, "color"),
                                                    StringArgumentType.getString(context, "icon")
                                                ))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("guidance")
                    .then(Commands.literal("disable")
                        .executes(context -> executeGuidance(context.getSource(), true))
                    )
                    .then(Commands.literal("enable")
                        .executes(context -> executeGuidance(context.getSource(), false))
                    )
                )
        );
    }

    private static int executeCreate(CommandSourceStack source, String title, String description, double x, double y, double z, String colorStr, String iconId) {
        ServerLevel level = source.getLevel();
        MarkerStorage storage = MarkerStorage.get(level);

        int color = parseColor(colorStr, source);

        // 验证图标 ID，无效或未提供则默认使用 point_block
        if (iconId == null || !PointMarkerIcon.isValid(iconId)) {
            iconId = PointMarkerIcon.POINT_BLOCK_ID;
        }

        Vec3 position = new Vec3(x, y, z);
        var marker = storage.createPointMarker(MarkerStorage.COMMAND_OWNER_ID, position, color, "",
                CreatePointMarkerPacket.TARGET_BLOCK, "", null,
                title, description, title, description,
                iconId, false);

        if (marker != null) {
            source.sendSuccess(() -> Component.translatable("message.airstrikepointers.command_marker_created",
                    title, String.format("%.1f", x), String.format("%.1f", y), String.format("%.1f", z))
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendFailure(Component.translatable("message.airstrikepointers.marker_limit_reached"));
        }

        return 1;
    }

    private static int executeGuidance(CommandSourceStack source, boolean disabled) {
        ServerLevel level = source.getLevel();
        MarkerStorage storage = MarkerStorage.get(level);

        UUID ownerId;
        if (source.getEntity() instanceof Player player) {
            ownerId = player.getUUID();
        } else {
            ownerId = MarkerStorage.COMMAND_OWNER_ID;
        }

        int count = storage.setGuidanceDisabledForOwner(ownerId, disabled);

        String statusKey = disabled ? "message.airstrikepointers.guidance_disabled" : "message.airstrikepointers.guidance_enabled";
        source.sendSuccess(() -> Component.translatable(statusKey, count)
                .withStyle(disabled ? ChatFormatting.RED : ChatFormatting.GREEN), true);

        return count;
    }

    private static int parseColor(String colorStr, CommandSourceStack source) {
        if (colorStr != null && !colorStr.isEmpty()) {
            try {
                String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
                if (hex.length() != 6) {
                    throw new IllegalArgumentException("Color must be 6 hex digits");
                }
                return Integer.parseInt(hex, 16);
            } catch (IllegalArgumentException ignored) {
                return 0xFF5555;
            }
        }

        if (source.getEntity() instanceof Player player) {
            Team team = player.getTeam();
            if (team != null && team.getColor() != ChatFormatting.RESET) {
                Integer teamColor = team.getColor().getColor();
                if (teamColor != null) {
                    return teamColor;
                }
            }
            int index = Math.abs(player.getUUID().hashCode()) % MARKER_COLORS.length;
            return MARKER_COLORS[index];
        }

        return 0xFF5555;
    }
}
