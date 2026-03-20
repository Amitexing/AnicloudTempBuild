package pl.flezy.tempbuild.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.flezy.tempbuild.TempBuild;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BlockDecayManager {
    public static final Map<Location, BlockData> placedBlocks = new HashMap<>();
    private static final Map<Location, Long> blockPlaceTime = new HashMap<>();
    private static final Map<Location, Long> blockDecayDuration = new HashMap<>();
    private static final Map<Location, Integer> blockEntityIds = new HashMap<>();

    public static void initialize() {

        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                Iterator<Map.Entry<Location, Long>> iterator = blockPlaceTime.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Location, Long> entry = iterator.next();
                    Location location = entry.getKey();
                    long placeTime = entry.getValue();
                    long elapsed = currentTime - placeTime;
                    long decayTimeMs = blockDecayDuration.get(location);

                    // Check if block still exists and matches expected type
                    Block block = location.getBlock();
                    BlockData expectedData = placedBlocks.get(location);

                    if (expectedData == null || block.getType() != expectedData.getMaterial()) {
                        clearBlock(location);
                        iterator.remove();
                        continue;
                    }

                    // End decay
                    if (elapsed >= decayTimeMs) {
                        removeBlock(location, block);
                        iterator.remove();
                        continue;
                    }

                    // Update animation
                    if (elapsed > 0) {
                        float progress = (float) elapsed / decayTimeMs;
                        int entityId = blockEntityIds.get(location);

                        // Send only to nearby players
                        for (Player player : location.getWorld().getPlayers()) {
                            player.sendBlockDamage(location, progress, entityId);
                        }
                    }
                }
            }
        }.runTaskTimer(TempBuild.getInstance(), 0, 1);
    }

    public static void addPlayerBlock(Location location) {
        Block block = location.getBlock();
        BlockData blockData = block.getBlockData();

        int decayTicks = TempBuild.getInstance().config.blockDecayTime * 20;
        long decayTimeMs = decayTicks * 50L;

        placedBlocks.put(location, blockData);
        blockPlaceTime.put(location, System.currentTimeMillis());
        blockDecayDuration.put(location, decayTimeMs);
        blockEntityIds.put(location, ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));

        if (blockData instanceof Bisected bisected) {
            if (bisected.getHalf() == Bisected.Half.BOTTOM) {
                Location topLocation = location.clone().add(0, 1, 0);
                Block topBlock = topLocation.getBlock();
                if (topBlock.getBlockData() instanceof Bisected) {
                    placedBlocks.put(topLocation, topBlock.getBlockData());
                    blockPlaceTime.put(topLocation, System.currentTimeMillis());
                    blockDecayDuration.put(topLocation, decayTimeMs);
                    blockEntityIds.put(topLocation, ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
                }
            }
        }
    }

    private static void clearBlock(Location location) {
        Integer entityId = blockEntityIds.remove(location);
        if (entityId != null) {
            for (Player player : location.getWorld().getPlayers()) {
                player.sendBlockDamage(location, 0f, entityId);
            }
        }
        placedBlocks.remove(location);
        blockDecayDuration.remove(location);
    }

    private static void removeBlock(Location location, Block block) {
        BlockData data = placedBlocks.get(location);
        clearBlock(location);

        if (TempBuild.getInstance().config.dropBlocks) {
            block.breakNaturally();
        }

        block.setType(Material.AIR);

        if (data instanceof Bisected bisected && bisected.getHalf() == Bisected.Half.BOTTOM) {
            Location topLocation = location.clone().add(0, 1, 0);
            clearBlock(topLocation);
        }
    }
}
