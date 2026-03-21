package pl.flezy.tempbuild.manager;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;

import java.util.Set;

public final class ReplaceablePlantHelper {
    private static final Set<Material> EXTRA_REPLACEABLE = Set.of(
            Material.GRASS,
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
                || material == Material.SHORT_GRASS
                || EXTRA_REPLACEABLE.contains(material)
                || Tag.SMALL_FLOWERS.isTagged(material)
                || Tag.TALL_FLOWERS.isTagged(material);
    }

    public static boolean isDoublePlant(BlockData data) {
        return data instanceof Bisected && isReplaceablePlant(data.getMaterial());
    }
}
