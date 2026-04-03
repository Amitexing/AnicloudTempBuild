package pl.flezy.tempbuild.manager;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Snow;

import java.util.EnumSet;
import java.util.Set;

public final class ReplaceablePlantHelper {
    private static final Set<Material> TALL_FLOWERS = EnumSet.of(
            Material.SUNFLOWER,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY
    );

    private static final Set<Material> EXTRA_REPLACEABLE = EnumSet.of(
            Material.GRASS,
            Material.SHORT_GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.DEAD_BUSH,
            Material.SWEET_BERRY_BUSH,
            Material.SUGAR_CANE,
            Material.BAMBOO,
            Material.CACTUS,
            Material.NETHER_SPROUTS,
            Material.CRIMSON_ROOTS,
            Material.WARPED_ROOTS,
            Material.HANGING_ROOTS,
            Material.PITCHER_CROP,
            Material.TORCHFLOWER_CROP
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
                || material == Material.SUGAR_CANE
                || material == Material.BAMBOO
                || material == Material.CACTUS
                || material == Material.SWEET_BERRY_BUSH
                || material == Material.TORCHFLOWER_CROP
                || material == Material.PITCHER_CROP;
    }
}
