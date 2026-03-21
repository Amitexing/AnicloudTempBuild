package pl.flezy.tempbuild.manager;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Snow;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ReplaceablePlantHelper {
    private static final Tag<Material> TALL_FLOWERS_TAG = resolveTag("TALL_FLOWERS");
    private static final Set<Material> EXTRA_REPLACEABLE = Stream.of(
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
            )
            .map(Material::matchMaterial)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());

    private ReplaceablePlantHelper() {
    }

    public static boolean isReplaceablePlant(Material material) {
        return material == Material.AIR
                ? false
                : Tag.FLOWERS.isTagged(material)
                || Tag.SAPLINGS.isTagged(material)
                || Tag.CROPS.isTagged(material)
                || EXTRA_REPLACEABLE.contains(material)
                || (TALL_FLOWERS_TAG != null && TALL_FLOWERS_TAG.isTagged(material));
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
        Material torchflowerCrop = Material.matchMaterial("TORCHFLOWER_CROP");
        Material pitcherCrop = Material.matchMaterial("PITCHER_CROP");
        return Tag.CROPS.isTagged(material)
                || Tag.SAPLINGS.isTagged(material)
                || Tag.FLOWERS.isTagged(material)
                || Tag.TALL_FLOWERS.isTagged(material)
                || material == Material.SUGAR_CANE
                || material == Material.BAMBOO
                || material == Material.CACTUS
                || material == Material.SWEET_BERRY_BUSH
                || material == torchflowerCrop
                || material == pitcherCrop;
    }

    @SuppressWarnings("unchecked")
    private static Tag<Material> resolveTag(String name) {
        try {
            return (Tag<Material>) Tag.class.getField(name).get(null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }
}
