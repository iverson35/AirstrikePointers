package dev.ignis.airstrikepointer.markers;

import dev.ignis.airstrikepointer.AirstrikePointers;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * 点标记图标注册中心。
 * 每个图标有唯一 ID 和对应的纹理路径。
 * 未来可通过 register() 扩展更多图标类型。
 */
public class PointMarkerIcon {
    // 预定义图标 ID
    public static final String POINT_ID = "point";
    public static final String POINT_BLOCK_ID = "point_block";
    public static final String ICON_ATTACK_ID = "icon_attack";
    public static final String ICON_CAUTION_ID = "icon_caution";
    public static final String ICON_DANGER_ID = "icon_danger";
    public static final String ICON_DEFEND_ID = "icon_defend";
    public static final String ICON_LIKE_ID = "icon_like";
    public static final String ICON_PUZZLED_ID = "icon_puzzled";
    public static final String ICON_REFUSE_ID = "icon_refuse";
    public static final String ICON_SATISFIED_ID = "icon_satisfied";

    private static final Map<String, ResourceLocation> ICONS = new LinkedHashMap<>();

    // 预注册两种图标
    @SuppressWarnings("removal")
    public static final ResourceLocation POINT_TEXTURE = register(POINT_ID, "textures/marker/point.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation POINT_BLOCK_TEXTURE = register(POINT_BLOCK_ID, "textures/marker/point_block.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation ICON_ATTACK_TEXTURE = register(ICON_ATTACK_ID, "textures/marker/icon_attack.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation ICON_CAUTION_TEXTURE = register(ICON_CAUTION_ID, "textures/marker/icon_caution.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation ICON_DANGER_TEXTURE = register(ICON_DANGER_ID, "textures/marker/icon_danger.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation ICON_DEFEND_TEXTURE = register(ICON_DEFEND_ID, "textures/marker/icon_defend.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation ICON_LIKE_TEXTURE = register(ICON_LIKE_ID, "textures/marker/icon_like.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation ICON_PUZZLED_TEXTURE = register(ICON_PUZZLED_ID, "textures/marker/icon_puzzled.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation ICON_REFUSE_TEXTURE = register(ICON_REFUSE_ID, "textures/marker/icon_refuse.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation ICON_SATISFIED_TEXTURE = register(ICON_SATISFIED_ID, "textures/marker/icon_satisfied.png");

    /**
     * 注册一个图标。
     * @param id 图标唯一标识
     * @param path 纹理路径（相对于 assets/airstrikepointers/）
     * @return 对应的 ResourceLocation
     */
    @SuppressWarnings("removal")
    public static ResourceLocation register(String id, String path) {
        ResourceLocation texture = new ResourceLocation(AirstrikePointers.MODID, path);
        ICONS.put(id, texture);
        return texture;
    }

    /**
     * 根据图标 ID 获取纹理。
     * @param iconId 图标 ID，如果为 null 或不存在则返回默认 point 纹理
     */
    public static ResourceLocation getTexture(String iconId) {
        if (iconId == null) return POINT_TEXTURE;
        return ICONS.getOrDefault(iconId, POINT_TEXTURE);
    }

    /**
     * 根据目标类型返回默认图标 ID。
     * @param isEntity 是否为实体标记
     */
    public static String getDefaultIconId(boolean isEntity) {
        return isEntity ? POINT_ID : POINT_BLOCK_ID;
    }

    /**
     * 获取所有已注册的图标 ID。
     */
    public static Collection<String> getIconIds() {
        return Collections.unmodifiableSet(ICONS.keySet());
    }

    /**
     * 检查图标 ID 是否有效。
     */
    public static boolean isValid(String iconId) {
        return iconId != null && ICONS.containsKey(iconId);
    }
}
