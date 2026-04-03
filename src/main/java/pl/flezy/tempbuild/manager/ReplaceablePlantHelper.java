package pl.flezy.tempbuild.manager;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Snow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ReplaceablePlantHelper {
    private static final Set<Material> TALL_FLOWERS = materialsFromNames(
            "SUNFLOWER",
            "LILAC",
            "ROSE_BUSH",
            "PEONY"
    );

    private static final Set<Material> EXTRA_REPLACEABLE = materialsFromNames(
            "GRASS",
            "SHORT_GRASS",
            "TALL_GRASS",
            "FERN",
            "LARGE_FERN",
            "DEAD_BUSH",
            "SWEET_BERRY_BUSH",
            "SUGAR_CANE",
            "BAMBOO",
            "CACTUS",
            "NETHER_SPROUTS",
            "CRIMSON_ROOTS",
            "WARPED_ROOTS",
            "HANGING_ROOTS",
            "PITCHER_CROP",
            "TORCHFLOWER_CROP"
    );

    private ReplaceablePlantHelper() {
    }

    public static boolean isReplaceablePlant(Material material) {
        return material == Material.AIR
                ? false
                : Tag.FLOWERS.isTagged(material)
                || Tag.SAPLINGS.isTagged(material)
                || Tag.CROPS.isTagged(material)
                || EXTRA_REPLACEABLE.contains(material)
                || TALL_FLOWERS.contains(material);
    }

    public static boolean isDoublePlant(BlockData data) {
        return data instanceof Bisected && isReplaceablePlant(data.getMaterial());
    }

    public static boolean isReplaceableForTempBuild(BlockState state) {
        Material material = state.getType();
        if (isReplaceablePlant(material)) {
            return true;
        }

        if (material == Material.FIRE || material == Material.SOUL_FIRE) {
            return true;
        }

        if (material == Material.SNOW && state.getBlockData() instanceof Snow snow) {
            return snow.getLayers() < snow.getMaximumLayers();
        }

        return false;
    }

    public static boolean isPlantingMaterial(Material material) {
        return Tag.CROPS.isTagged(material)
                || Tag.SAPLINGS.isTagged(material)
                || Tag.FLOWERS.isTagged(material)
                || TALL_FLOWERS.contains(material)
                || EXTRA_REPLACEABLE.contains(material);
    }

    private static Set<Material> materialsFromNames(String... names) {
        Set<Material> result = new HashSet<>();
        Arrays.stream(names)
                .map(Material::matchMaterial)
                .filter(java.util.Objects::nonNull)
                .forEach(result::add);
        return result;
    }
}
