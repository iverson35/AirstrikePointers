package dev.ignis.airstrikepointer;

import dev.ignis.airstrikepointer.markers.MarkerStorage;
import dev.ignis.airstrikepointer.markers.PointMarker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuidanceSystem {
    private static final GuidanceSystem INSTANCE = new GuidanceSystem();
    
    public static GuidanceSystem getInstance() {
        return INSTANCE;
    }
    
    private int tickCounter = 0;
    private Set<ResourceLocation> guidanceEntityTypes = new HashSet<>();
    private boolean entityListDirty = true;

    private GuidanceSystem() {
        // 注册配置重载回调
        Config.setOnReload(() -> entityListDirty = true);
    }

    public void tick() {
        if (!Config.GUIDANCE_ENABLED.get()) return;

        tickCounter++;
        int interval = Config.GUIDANCE_INTERVAL.get();
        if (tickCounter % interval != 0) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // 更新制导实体类型缓存
        updateGuidanceEntityTypes();

        double horizontalRange = Config.GUIDANCE_HORIZONTAL_RANGE.get();
        double verticalRange = Config.GUIDANCE_VERTICAL_RANGE.get() / 2.0;
        double verticalOffset = Config.GUIDANCE_VERTICAL_OFFSET.get();
        double guidanceRatio = Config.GUIDANCE_RATIO.get();

        // 遍历所有维度
        for (ServerLevel level : server.getAllLevels()) {
            MarkerStorage storage = MarkerStorage.get(level);
            
            for (PointMarker marker : storage.getPointMarkers()) {
                Vec3 targetPos = marker.getPosition().add(0, verticalOffset, 0);

                // 搜索制导范围内的实体
                AABB searchArea = new AABB(
                        targetPos.x - horizontalRange, targetPos.y - verticalRange, targetPos.z - horizontalRange,
                        targetPos.x + horizontalRange, targetPos.y + verticalRange, targetPos.z + horizontalRange
                );

                List<Entity> projectiles = level.getEntities((Entity) null, searchArea, entity -> {
                    ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                    return guidanceEntityTypes.contains(entityId);
                });

                for (Entity projectile : projectiles) {
                    applyGuidance(projectile, marker.getPosition(), guidanceRatio);
                }
            }
        }
    }

    @SuppressWarnings("removal")
    private void updateGuidanceEntityTypes() {
        // 每100tick更新一次缓存，或配置被重载时
        if (!entityListDirty && tickCounter % 100 != 1) return;

        guidanceEntityTypes.clear();
        List<String> entityList = Config.GUIDANCE_ENTITY_LIST.get();
        for (String entityId : entityList) {
            try {
                ResourceLocation id = new ResourceLocation(entityId);
                guidanceEntityTypes.add(id);
            } catch (Exception e) {
                // 无效的实体ID，忽略
            }
        }
        entityListDirty = false;
    }

    private void applyGuidance(Entity projectile, Vec3 targetPos, double ratio) {
        Vec3 currentVel = projectile.getDeltaMovement();
        Vec3 toTarget = targetPos.subtract(projectile.position()).normalize();

        // 新速度 = 原速度 * (1 - ratio) + 目标方向 * 原速度长度 * ratio
        double speed = currentVel.length();
        if (speed < 0.01) return;

        Vec3 newVel = currentVel.scale(1 - ratio).add(toTarget.scale(speed * ratio));
        projectile.setDeltaMovement(newVel);

        // 同步到客户端
        projectile.hurtMarked = true;
    }
}
