package dev.ignis.airstrikepointer.items;

import dev.ignis.airstrikepointer.markers.MarkerStorage;
import dev.ignis.airstrikepointer.markers.PointMarkerIcon;
import dev.ignis.airstrikepointer.network.CreatePointMarkerPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class PocketLaserPointerItem extends Item {

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

    public PocketLaserPointerItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // 很长的使用时间，用于区分长按/短按
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("tooltip.airstrikepointers.pocket_short_press").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.translatable("tooltip.airstrikepointers.pocket_long_press").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.airstrikepointers.clear_markers").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player.isShiftKeyDown()) {
            // Shift+右键：清除所有标记
            if (!level.isClientSide) {
                MarkerStorage.get(level).clearMarkersByOwner(player.getUUID());
                player.displayClientMessage(Component.translatable("message.airstrikepointers.markers_cleared").withStyle(ChatFormatting.GREEN), true);
                player.getCooldowns().removeCooldown(this);
            }
            return InteractionResultHolder.success(stack);
        }

        // 开始使用物品以检测长按/短按
        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) return;
        if (player.isShiftKeyDown()) return;

        int useTicks = getUseDuration(stack) - timeCharged;
        final int SHORT_PRESS_THRESHOLD = 5; // ~250ms

        if (useTicks < SHORT_PRESS_THRESHOLD) {
            // 短按：执行标记逻辑
            performMarking(player, stack);
        } else {
            // 长按：留空实现
        }
    }

    private void performMarking(Player player, ItemStack stack) {
        Level level = player.level();

        // 射线检测目标
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
        String iconId;

        if (entityHitResult != null) {
            // 命中实体
            Entity hitEntity = entityHitResult.getEntity();
            // 如果命中的是多部分实体的部件，则转而标记父实体
            if (hitEntity instanceof PartEntity<?> partEntity) {
                hitEntity = partEntity.getParent();
            }
            targetPos = entityHitResult.getLocation();
            targetType = CreatePointMarkerPacket.TARGET_ENTITY;
            entityName = hitEntity.getDisplayName().getString();
            targetEntityId = hitEntity.getUUID();
            iconId = determineIconForEntity(player, hitEntity);
        } else {
            // 检测方块
            BlockHitResult blockHitResult = (BlockHitResult) player.pick(300.0, 0.0f, false);
            if (blockHitResult.getType() == HitResult.Type.BLOCK) {
                targetPos = blockHitResult.getLocation();
                targetType = CreatePointMarkerPacket.TARGET_BLOCK;
                iconId = determineIconForBlock(player, level, blockHitResult);
            } else {
                // 未命中任何目标
                targetPos = endPos;
                targetType = CreatePointMarkerPacket.TARGET_MISS;
                iconId = PointMarkerIcon.ICON_DOT_ID;
            }
        }

        int color = getPlayerColor(player);
        String teamName = getPlayerTeamName(player);
        MarkerStorage storage = MarkerStorage.get(level);

        // 标点前清除该玩家之前的所有无制导点（由袖珍激光笔创建）
        storage.clearNonGuidedMarkersByOwner(player.getUUID());

        String itemName = stack.hasCustomHoverName() ? stack.getDisplayName().getString() : null;

        var marker = storage.createPointMarker(player.getUUID(), targetPos, color, teamName,
                targetType, entityName, targetEntityId, player.getDisplayName().getString(),
                itemName, null, null, iconId, true); // guidanceDisabled=true

        if (marker != null) {
            player.displayClientMessage(Component.translatable("message.airstrikepointers.point_marked").withStyle(ChatFormatting.GREEN), true);
        } else {
            player.displayClientMessage(Component.translatable("message.airstrikepointers.marker_limit_reached").withStyle(ChatFormatting.RED), true);
        }
    }

    /**
     * 根据标记到的方块确定图标。
     * 判断优先级：
     * 1. 玩家血量 < 50% 且距离脚底 ≤ 2m → icon_danger
     * 2. 有方块实体且 id 包含 loot/chest/barrel → icon_satisfied
     * 3. 默认 → point_block
     */
    private String determineIconForBlock(Player player, Level level, BlockHitResult blockHit) {
        BlockPos blockPos = blockHit.getBlockPos();
        BlockState blockState = level.getBlockState(blockPos);
        Vec3 hitPos = blockHit.getLocation();

        // 优先级1：玩家血量 < 50% 且距离脚底 ≤ 2m
        double feetDistance = player.position().distanceTo(hitPos);
        if (player.getHealth() < player.getMaxHealth() * 0.5f && feetDistance <= 2.0) {
            return PointMarkerIcon.ICON_DANGER_ID;
        }

        // 优先级2：有方块实体且方块 id 包含 loot/chest/barrel
        if (level.getBlockEntity(blockPos) != null) {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
            if (blockId != null) {
                String idStr = blockId.toString().toLowerCase();
                if (idStr.contains("loot") || idStr.contains("chest") || idStr.contains("barrel")) {
                    return PointMarkerIcon.ICON_SATISFIED_ID;
                }
            }
        }

        // 默认：icon_dot
        return PointMarkerIcon.ICON_DOT_ID;
    }

    /**
     * 根据标记到的实体确定图标。
     * 判断优先级：
     * 1. 敌对生物 + 最大生命值 ≥ 100 → icon_danger
     * 2. 敌对生物 → icon_caution
     * 3. 另一名玩家 → icon_like
     * 4. 非敌对生物 → icon_defend
     * 5. 默认 → point
     */
    private String determineIconForEntity(Player player, Entity entity) {
        if (entity instanceof LivingEntity) {
            boolean isHostile = entity instanceof Monster;

            if (isHostile) {
                LivingEntity living = (LivingEntity) entity;
                // Boss级：最大生命值 ≥ 100
                if (living.getMaxHealth() >= 100.0) {
                    return PointMarkerIcon.ICON_DANGER_ID;
                }
                // 普通敌对生物
                return PointMarkerIcon.ICON_CAUTION_ID;
            }

            // 另一名玩家
            if (entity instanceof Player) {
                return PointMarkerIcon.ICON_LIKE_ID;
            }

            // 非敌对生物（动物、村民等）
            return PointMarkerIcon.ICON_DEFEND_ID;
        }

        // 非生物实体（箭、物品展示框等）默认 icon_dot
        return PointMarkerIcon.ICON_DOT_ID;
    }

    private static int getPlayerColor(Player player) {
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

    private static String getPlayerTeamName(Player player) {
        Team team = player.getTeam();
        return team != null ? team.getName() : "";
    }
}
