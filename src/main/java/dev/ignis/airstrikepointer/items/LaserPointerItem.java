package dev.ignis.airstrikepointer.items;

import dev.ignis.airstrikepointer.Config;
import dev.ignis.airstrikepointer.markers.MarkerStorage;
import dev.ignis.airstrikepointer.network.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class LaserPointerItem extends Item {
    private static final String MODE_KEY = "Mode";
    private static final String PATH_MARKER_ID_KEY = "PathMarkerId";

    public enum Mode {
        POINT("mode.airstrikepointers.point", ChatFormatting.GREEN),
        PATH("mode.airstrikepointers.path", ChatFormatting.BLUE);

        private final String translationKey;
        private final ChatFormatting color;

        Mode(String translationKey, ChatFormatting color) {
            this.translationKey = translationKey;
            this.color = color;
        }

        public String getTranslationKey() { return translationKey; }
        public ChatFormatting getColor() { return color; }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public LaserPointerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new dev.ignis.airstrikepointer.client.LaserPointerItemExtensions());
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        Mode mode = getMode(stack);
        String modeKey = switch (mode) {
            case POINT -> "mode.airstrikepointers.point";
            case PATH -> "mode.airstrikepointers.path";
        };
        tooltipComponents.add(Component.translatable("tooltip.airstrikepointers.mode", 
                Component.translatable(modeKey).withStyle(mode.getColor())));
        tooltipComponents.add(Component.translatable("tooltip.airstrikepointers.switch_mode").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.airstrikepointers.clear_markers").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.airstrikepointers.cancel_mark").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // 很长的使用时间
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player.isShiftKeyDown()) {
            // Shift+右键切换模式（POINT <-> PATH）
            if (!level.isClientSide) {
                switchMode(stack);
                Mode newMode = getMode(stack);
                player.displayClientMessage(Component.translatable("message.airstrikepointers.mode_switched",
                        Component.translatable(newMode.getTranslationKey()).withStyle(newMode.getColor())), true);
            }
            return InteractionResultHolder.success(stack);
        }

        // 开始使用（望远镜视角）
        player.startUsingItem(usedHand);
        // 播放望远镜使用声音
        level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                net.minecraft.sounds.SoundEvents.SPYGLASS_USE, 
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        // 松开右键时执行标记操作
        if (livingEntity instanceof Player player && !level.isClientSide) {
            // 潜行模式下停止使用，什么也不做
            if (player.isShiftKeyDown()) {
                return;
            }
            
            // 检查冷却（路径模式标记起点时不检查冷却）
            Mode mode = getMode(stack);
            boolean isPathStart = (mode == Mode.PATH && getPathMarkerId(stack) == null);
            
            int cooldownTicks = Config.MARKER_COOLDOWN_TICKS.get();
            if (cooldownTicks > 0 && !isPathStart && player.getCooldowns().isOnCooldown(this)) {
                player.displayClientMessage(Component.translatable("message.airstrikepointers.cooldown_active").withStyle(ChatFormatting.RED), true);
                return;
            }
            
            performMarking(player, stack);
            
            // 添加冷却（路径模式标记起点时不添加冷却）
            if (cooldownTicks > 0 && !isPathStart) {
                player.getCooldowns().addCooldown(this, cooldownTicks);
            }
            
            // 播放结束使用声音 (note block)
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void performMarking(Player player, ItemStack stack) {
        Level level = player.level();
        Mode mode = getMode(stack);

        // 检测目标
        Vec3 eyePos = player.getEyePosition(0.0f);
        Vec3 lookVec = player.getViewVector(0.0f);
        Vec3 endPos = eyePos.add(lookVec.x * 300.0, lookVec.y * 300.0, lookVec.z * 300.0);

        var entityHitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                level, player, eyePos, endPos,
                player.getBoundingBox().expandTowards(lookVec.scale(300.0)).inflate(1.0),
                entity -> !entity.isSpectator() && entity.isPickable()
        );

        Vec3 targetPos;
        int targetType;
        String entityName = "";
        UUID targetEntityId = null;

        if (entityHitResult != null) {
            targetPos = entityHitResult.getLocation();
            targetType = CreatePointMarkerPacket.TARGET_ENTITY;
            entityName = entityHitResult.getEntity().getDisplayName().getString();
            targetEntityId = entityHitResult.getEntity().getUUID();
        } else {
            HitResult blockHitResult = player.pick(300.0, 0.0f, false);
            if (blockHitResult.getType() == HitResult.Type.BLOCK) {
                targetPos = blockHitResult.getLocation();
                targetType = CreatePointMarkerPacket.TARGET_BLOCK;
            } else {
                targetPos = endPos;
                targetType = CreatePointMarkerPacket.TARGET_MISS;
            }
        }

        int color = getPlayerColor(player);
        String teamName = getPlayerTeamName(player);
        MarkerStorage storage = MarkerStorage.get(level);

        if (mode == Mode.POINT) {
            var marker = storage.createPointMarker(player.getUUID(), targetPos, color, teamName, targetType, entityName, targetEntityId, player.getDisplayName().getString());
            if (marker != null) {
                player.displayClientMessage(Component.translatable("message.airstrikepointers.point_marked").withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(Component.translatable("message.airstrikepointers.marker_limit_reached").withStyle(ChatFormatting.RED), true);
            }
        } else if (mode == Mode.PATH) {
            UUID existingPathId = getPathMarkerId(stack);
            if (existingPathId != null) {
                storage.completePathMarker(existingPathId, targetPos, player.getDisplayName().getString());
                clearPathMarkerId(stack);
                player.displayClientMessage(Component.translatable("message.airstrikepointers.path_created").withStyle(ChatFormatting.GREEN), true);
            } else {
                var marker = storage.createPathStart(player.getUUID(), targetPos, color, teamName, player.getDisplayName().getString());
                if (marker != null) {
                    setPathMarkerId(stack, marker.getMarkerId());
                    player.displayClientMessage(Component.translatable("message.airstrikepointers.path_start_set").withStyle(ChatFormatting.YELLOW), true);
                    // 向创建者的客户端发送预览包（只在本地显示起点标记）
                    int lifetimeTicks = marker.getRemainingTicks();
                    CreatePathMarkerPacket previewPacket = new CreatePathMarkerPacket(
                            marker.getMarkerId(), player.getUUID(), targetPos, null,
                            (float) targetPos.y, color, teamName, lifetimeTicks, true, 0);
                    NetworkHandler.CHANNEL.sendTo(previewPacket, ((ServerPlayer) player).connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                } else {
                    player.displayClientMessage(Component.translatable("message.airstrikepointers.marker_limit_reached").withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    public static void switchMode(ItemStack stack) {
        Mode currentMode = getMode(stack);
        Mode nextMode = currentMode.next();
        setMode(stack, nextMode);
    }

    private static Mode getMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(MODE_KEY)) {
            return Mode.POINT;
        }
        try {
            return Mode.valueOf(tag.getString(MODE_KEY));
        } catch (IllegalArgumentException e) {
            return Mode.POINT;
        }
    }

    private static void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateTag().putString(MODE_KEY, mode.name());
    }

    private static UUID getPathMarkerId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(PATH_MARKER_ID_KEY)) {
            return tag.getUUID(PATH_MARKER_ID_KEY);
        }
        return null;
    }

    private static void setPathMarkerId(ItemStack stack, UUID markerId) {
        stack.getOrCreateTag().putUUID(PATH_MARKER_ID_KEY, markerId);
    }

    private static void clearPathMarkerId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(PATH_MARKER_ID_KEY);
        }
    }

    // High-visibility color palette for unteamed players
    private static final int[] MARKER_COLORS = {
            0xFF5555, // Red
            0xFFAA00, // Gold
            0xFFFF55, // Yellow
            0x55FF55, // Green
            0x55FFFF, // Aqua
            0x5555FF, // Blue
            0xFF55FF, // Light Purple
            0xFF8800, // Orange
            0x00FF88, // Mint
            0xFF0088, // Pink
            0x88FF00, // Lime
            0x0088FF  // Sky Blue
    };

    private static int getPlayerColor(Player player) {
        Team team = player.getTeam();
        if (team != null && team.getColor() != ChatFormatting.RESET) {
            Integer teamColor = team.getColor().getColor();
            if (teamColor != null) {
                return teamColor;
            }
        }

        // Use UUID hash to select from predefined high-visibility colors
        int index = Math.abs(player.getUUID().hashCode()) % MARKER_COLORS.length;
        return MARKER_COLORS[index];
    }

    private static String getPlayerTeamName(Player player) {
        Team team = player.getTeam();
        return team != null ? team.getName() : "";
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && isSelected && entity instanceof Player player) {
            UUID pathMarkerId = getPathMarkerId(stack);
            if (pathMarkerId != null) {
                MarkerStorage storage = MarkerStorage.get(level);
                boolean exists = storage.getPathMarker(pathMarkerId) != null;
                if (!exists) {
                    clearPathMarkerId(stack);
                }
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
