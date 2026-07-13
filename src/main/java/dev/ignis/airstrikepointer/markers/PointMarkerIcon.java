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

    private static final Map<String, ResourceLocation> ICONS = new LinkedHashMap<>();

    // 预注册两种图标
    @SuppressWarnings("removal")
    public static final ResourceLocation POINT_TEXTURE = register(POINT_ID, "textures/marker/point.png");
    @SuppressWarnings("removal")
    public static final ResourceLocation POINT_BLOCK_TEXTURE = register(POINT_BLOCK_ID, "textures/marker/point_block.png");

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
