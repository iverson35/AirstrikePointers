package dev.ignis.airstrikepointer.network;

import dev.ignis.airstrikepointer.items.LaserPointerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UsePointerPacket(double targetX, double targetY, double targetZ, int targetType, String entityName) {
    public static final int TARGET_BLOCK = 0;
    public static final int TARGET_ENTITY = 1;
    public static final int TARGET_MISS = 2;

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(targetX);
        buf.writeDouble(targetY);
        buf.writeDouble(targetZ);
        buf.writeInt(targetType);
        buf.writeUtf(entityName != null ? entityName : "");
    }

    public static UsePointerPacket decode(FriendlyByteBuf buf) {
        return new UsePointerPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof LaserPointerItem)) return;

            HitResult hitResult;
            if (targetType == TARGET_ENTITY) {
                hitResult = new EntityHitResult(null, new net.minecraft.world.phys.Vec3(targetX, targetY, targetZ));
            } else if (targetType == TARGET_BLOCK) {
                hitResult = new BlockHitResult(new net.minecraft.world.phys.Vec3(targetX, targetY, targetZ), null, null, false);
            } else {
                hitResult = new HitResult(new net.minecraft.world.phys.Vec3(targetX, targetY, targetZ)) {
                    @Override
                    public Type getType() { return Type.MISS; }
                };
            }

            LaserPointerItem.onServerUse(player, stack, hitResult, entityName);
        });
        ctx.get().setPacketHandled(true);
    }
}
