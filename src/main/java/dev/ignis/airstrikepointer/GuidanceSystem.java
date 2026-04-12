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
import java.util.UUID;

public class GuidanceSystem {
    private static final GuidanceSystem INSTANCE = new GuidanceSystem();
    
    public static GuidanceSystem getInstance() {
        return INSTANCE;
    }
    
    private int tickCounter = 0;
    private Set<ResourceLocation> guidanceEntityTypes = new HashSet<>();
    private boolean entityListDirty = true;
    
    // CBC 支持
    private final boolean cbcInstalled;
    private final Class<?> cbcProjectileClass;

    private GuidanceSystem() {
        // 注册配置重载回调
        Config.setOnReload(() -> entityListDirty = true);
        
        // 检测 CBC 是否安装
        Class<?> cbcClass = null;
        try {
            cbcClass = Class.forName("rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile");
        } catch (ClassNotFoundException e) {
            // CBC 未安装
        }
        cbcInstalled = cbcClass != null;
        cbcProjectileClass = cbcClass;
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

        double horizontalRange = Config.GUIDANCE_HORIZONTAL_RANGE.get() / 2.0;
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

                boolean guideCbcShells = cbcInstalled && Config.GUIDANCE_GUIDE_CBC_SHELLS.get();
                
                List<Entity> projectiles = level.getEntities((Entity) null, searchArea, entity -> {
                    // 检查是否在配置列表中
                    ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                    if (guidanceEntityTypes.contains(entityId)) return true;
                    
                    // 检查是否是 CBC 炮弹
                    if (guideCbcShells && cbcProjectileClass.isInstance(entity)) return true;
                    
                    return false;
                });

                for (Entity projectile : projectiles) {
                    applyGuidance(projectile, marker.getPosition(), guidanceRatio, marker.getMarkerId());
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

    private static final String GUIDANCE_MARKER_TAG = "airstrikepointers:guided_by";
    private static final double MIN_SPEED = 0.1;

    private void applyGuidance(Entity projectile, Vec3 targetPos, double ratio, UUID markerId) {
        // 检查是否已被其他标记制导
        if (projectile.getPersistentData().hasUUID(GUIDANCE_MARKER_TAG)) {
            UUID currentMarker = projectile.getPersistentData().getUUID(GUIDANCE_MARKER_TAG);
            if (!currentMarker.equals(markerId)) {
                return; // 已被其他标记制导，跳过
            }
        } else {
            // 首次制导，记录标记ID
            projectile.getPersistentData().putUUID(GUIDANCE_MARKER_TAG, markerId);
        }

        Vec3 currentVel = projectile.getDeltaMovement();
        Vec3 toTarget = targetPos.subtract(projectile.position()).normalize();

        // 新速度 = 原速度 * (1 - ratio) + 目标方向 * 速度 * ratio
        double speed = Math.max(currentVel.length(), MIN_SPEED);
        
        Vec3 newVel = currentVel.scale(1 - ratio).add(toTarget.scale(speed * ratio));
        
        // 保证最小速度
        if (newVel.length() < MIN_SPEED) {
            newVel = newVel.normalize().scale(MIN_SPEED);
        }
        
        projectile.setDeltaMovement(newVel);

        // 同步到客户端
        projectile.hurtMarked = true;
    }
}
