package dev.ignis.airstrikepointer;

import dev.ignis.airstrikepointer.markers.MarkerStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AirstrikePointers.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 遍历所有维度，更新标记
        for (ServerLevel level : event.getServer().getAllLevels()) {
            MarkerStorage.get(level).tick();
        }
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
}
