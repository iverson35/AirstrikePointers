package dev.ignis.airstrikepointer.items;

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
        POINT("坐标点模式", ChatFormatting.GREEN),
        PATH("航向模式", ChatFormatting.BLUE),
        CLEAR("清除模式", ChatFormatting.RED);

        private final String displayName;
        private final ChatFormatting color;

        Mode(String displayName, ChatFormatting color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
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
            case CLEAR -> "mode.airstrikepointers.clear";
        };
        tooltipComponents.add(Component.translatable("tooltip.airstrikepointers.mode", 
                Component.translatable(modeKey).withStyle(mode.getColor())));
        tooltipComponents.add(Component.translatable("tooltip.airstrikepointers.switch_mode").withStyle(ChatFormatting.GRAY));
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
            // Shift+右键直接切换模式
            if (!level.isClientSide) {
                switchMode(stack);
                player.displayClientMessage(Component.literal("模式切换为: " + getMode(stack).getDisplayName()).withStyle(getMode(stack).getColor()), true);
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
            performMarking(player, stack);
            // 播放结束使用声音 (note block)
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void performMarking(Player player, ItemStack stack) {
        Level level = player.level();
        Mode mode = getMode(stack);

        if (mode == Mode.CLEAR) {
            MarkerStorage.get(level).clearMarkersByOwner(player.getUUID());
            player.displayClientMessage(Component.literal("已清除所有标记").withStyle(ChatFormatting.GREEN), true);
            return;
        }

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

        if (entityHitResult != null) {
            targetPos = entityHitResult.getLocation();
            targetType = CreatePointMarkerPacket.TARGET_ENTITY;
            entityName = entityHitResult.getEntity().getDisplayName().getString();
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
            storage.createPointMarker(player.getUUID(), targetPos, color, teamName, targetType, entityName, player.getDisplayName().getString());
            player.displayClientMessage(Component.literal("已标记坐标点").withStyle(ChatFormatting.GREEN), true);
        } else if (mode == Mode.PATH) {
            UUID existingPathId = getPathMarkerId(stack);
            if (existingPathId != null) {
                storage.completePathMarker(existingPathId, targetPos, player.getDisplayName().getString());
                clearPathMarkerId(stack);
                player.displayClientMessage(Component.literal("航向路径已创建").withStyle(ChatFormatting.GREEN), true);
            } else {
                var marker = storage.createPathStart(player.getUUID(), targetPos, color, teamName, player.getDisplayName().getString());
                if (marker != null) {
                    setPathMarkerId(stack, marker.getMarkerId());
                    player.displayClientMessage(Component.literal("已设置起点，长按右键设置终点").withStyle(ChatFormatting.YELLOW), true);
                    // 向创建者的客户端发送预览包（只在本地显示起点标记）
                    int lifetimeTicks = marker.getRemainingTicks();
                    CreatePathMarkerPacket previewPacket = new CreatePathMarkerPacket(
                            marker.getMarkerId(), player.getUUID(), targetPos, null,
                            (float) targetPos.y, color, teamName, lifetimeTicks, true, 0);
                    NetworkHandler.CHANNEL.sendTo(previewPacket, ((ServerPlayer) player).connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                } else {
                    player.displayClientMessage(Component.literal("标记数量已达上限").withStyle(ChatFormatting.RED), true);
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

    private static int getPlayerColor(Player player) {
        Team team = player.getTeam();
        if (team != null && team.getColor() != ChatFormatting.RESET) {
            Integer teamColor = team.getColor().getColor();
            if (teamColor != null) {
                return teamColor;
            }
        }

        int hash = player.getUUID().hashCode();
        int r = 64 + (Math.abs(hash) % 192);
        int g = 64 + (Math.abs(hash >> 8) % 192);
        int b = 64 + (Math.abs(hash >> 16) % 192);
        return (r << 16) | (g << 8) | b;
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
