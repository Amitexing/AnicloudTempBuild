package pl.flezy.tempbuild.manager;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ReplaceablePlantHelper {
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
                || Tag.TALL_FLOWERS.isTagged(material);
    }

    public static boolean isDoublePlant(BlockData data) {
        return data instanceof Bisected && isReplaceablePlant(data.getMaterial());
    }
}
