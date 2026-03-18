package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.items.LaserPointerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ModeSwitchPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ModeSwitchPacket decode(FriendlyByteBuf buf) {
        return new ModeSwitchPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof LaserPointerItem) {
                LaserPointerItem.switchMode(stack);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
