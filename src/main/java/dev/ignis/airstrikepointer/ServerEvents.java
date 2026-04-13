package dev.ignis.airstrikepointer;

import dev.ignis.airstrikepointer.items.LaserPointerItem;
import dev.ignis.airstrikepointer.items.ModItems;
import dev.ignis.airstrikepointer.markers.MarkerStorage;
import dev.ignis.airstrikepointer.network.NetworkHandler;
import dev.ignis.airstrikepointer.network.RequestClearMarkersPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 只在主世界执行标记tick（避免多维度重复递减）
        ServerLevel overworld = event.getServer().overworld();
        if (overworld != null) {
            MarkerStorage.get(overworld).tick();
        }
        
        // 制导系统tick
        GuidanceSystem.getInstance().tick();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 向新加入的玩家同步所有标记
            for (ServerLevel level : player.getServer().getAllLevels()) {
                MarkerStorage.get(level).syncToPlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        // 监听左键点击空气事件（客户端触发，需要发送给服务端）
        if (event.getEntity().isShiftKeyDown()) {
            ItemStack stack = event.getEntity().getMainHandItem();
            if (stack.getItem() instanceof LaserPointerItem) {
                // 发送网络包到服务端请求清除标记
                NetworkHandler.CHANNEL.send(PacketDistributor.SERVER.noArg(), new RequestClearMarkersPacket());
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isShiftKeyDown()) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof LaserPointerItem) {
                // 取消事件，阻止破坏方块
                event.setCanceled(true);
                // 清除标记
                MarkerStorage.get(player.level()).clearMarkersByOwner(player.getUUID());
                player.displayClientMessage(Component.translatable("message.airstrikepointers.markers_cleared").withStyle(ChatFormatting.GREEN), true);
                // 清除冷却，让指示器立即可用
                player.getCooldowns().removeCooldown(ModItems.LASER_POINTER.get());
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isShiftKeyDown()) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof LaserPointerItem) {
                // 取消事件，阻止攻击实体
                event.setCanceled(true);
                // 清除标记
                MarkerStorage.get(player.level()).clearMarkersByOwner(player.getUUID());
                player.displayClientMessage(Component.translatable("message.airstrikepointers.markers_cleared").withStyle(ChatFormatting.GREEN), true);
                // 清除冷却，让指示器立即可用
                player.getCooldowns().removeCooldown(ModItems.LASER_POINTER.get());
            }
        }
    }
}
