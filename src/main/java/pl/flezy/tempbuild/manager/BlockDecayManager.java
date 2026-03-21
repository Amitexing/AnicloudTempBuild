package pl.flezy.tempbuild.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.flezy.tempbuild.TempBuild;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BlockDecayManager {
    private static final int EXPIRATION_CHECK_TICKS = 20;
    private static final int ANIMATION_UPDATE_TICKS = 20;
    private static final int MAX_ANIMATION_BLOCKS = 500;

    public static final Map<Location, BlockData> placedBlocks = new HashMap<>();
    private static final Map<Location, Long> blockPlaceTime = new HashMap<>();
    private static final Map<Location, Long> blockDecayDuration = new HashMap<>();
    private static final Map<Location, Integer> blockEntityIds = new HashMap<>();

    private static final Map<Location, Long> blockExpireAt = new HashMap<>();
    private static final NavigableMap<Long, Set<Location>> expirationBuckets = new TreeMap<>();

    public static void initialize() {
        new BukkitRunnable() {
            @Override
            public void run() {
                processExpiredBlocks();
            }
        }.runTaskTimer(TempBuild.getInstance(), EXPIRATION_CHECK_TICKS, EXPIRATION_CHECK_TICKS);

        new BukkitRunnable() {
            @Override
            public void run() {
                updateDamageAnimations();
            }
        }.runTaskTimer(TempBuild.getInstance(), ANIMATION_UPDATE_TICKS, ANIMATION_UPDATE_TICKS);
    }

    public static void loadPersistedBlocks() {
        TempBlockStorage storage = TempBuild.getInstance().tempBlockStorage;
        if (storage == null) {
            return;
        }

        for (TempBlockStorage.StoredTempBlock stored : storage.loadAll()) {
            Location location = stored.location();
            Block block = location.getBlock();

            long expiresAt = stored.placeTimeMs() + stored.decayDurationMs();
            if (System.currentTimeMillis() >= expiresAt) {
                storage.delete(location);
                continue;
            }

            if (block.getType() != stored.blockData().getMaterial()) {
                storage.delete(location);
                continue;
            }

            trackBlock(location, stored.blockData(), stored.placeTimeMs(), stored.decayDurationMs(), false);
        }
    }

    public static void addPlayerBlock(Location location) {
        Block block = location.getBlock();
        BlockData blockData = block.getBlockData();

        int decayTicks = TempBuild.getInstance().config.getDecayTimeMinutes(block.getType()) * 20 * 60;
        long decayTimeMs = decayTicks * 50L;
        long now = System.currentTimeMillis();

        trackBlock(location, blockData, now, decayTimeMs, true);

        if (blockData instanceof Bisected bisected && bisected.getHalf() == Bisected.Half.BOTTOM) {
            Location topLocation = location.clone().add(0, 1, 0);
            Block topBlock = topLocation.getBlock();
            if (topBlock.getBlockData() instanceof Bisected) {
                trackBlock(topLocation, topBlock.getBlockData(), now, decayTimeMs, true);
            }
        }

        org.bukkit.Bukkit.getScheduler().runTask(TempBuild.getInstance(), () -> {
            updateTrackedBlockData(location);
            updateTrackedBlockData(location.clone().add(1, 0, 0));
            updateTrackedBlockData(location.clone().add(-1, 0, 0));
            updateTrackedBlockData(location.clone().add(0, 0, 1));
            updateTrackedBlockData(location.clone().add(0, 0, -1));
        });
    }

    public static void untrackBlock(Location location) {
        clearBlock(location);
    }

    public static void updateTrackedBlockData(Location location) {
        if (!placedBlocks.containsKey(location)) {
            return;
        }

        BlockData currentData = location.getBlock().getBlockData();
        placedBlocks.put(location, currentData);

        Long placeTime = blockPlaceTime.get(location);
        Long decayDuration = blockDecayDuration.get(location);
        if (placeTime != null && decayDuration != null && TempBuild.getInstance().tempBlockStorage != null) {
            TempBuild.getInstance().tempBlockStorage.upsert(location, currentData, placeTime, decayDuration);
        }
    }

    private static void trackBlock(Location location, BlockData data, long placeTime, long decayTimeMs, boolean persist) {
        unregisterExpiration(location);

        placedBlocks.put(location, data);
        blockPlaceTime.put(location, placeTime);
        blockDecayDuration.put(location, decayTimeMs);
        blockEntityIds.put(location, ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));

        long expireAt = placeTime + decayTimeMs;
        blockExpireAt.put(location, expireAt);
        expirationBuckets.computeIfAbsent(expireAt, ignored -> new HashSet<>()).add(location);

        if (persist && TempBuild.getInstance().tempBlockStorage != null) {
            TempBuild.getInstance().tempBlockStorage.upsert(location, data, placeTime, decayTimeMs);
        }
    }

    private static void processExpiredBlocks() {
        long now = System.currentTimeMillis();

        while (!expirationBuckets.isEmpty()) {
            Map.Entry<Long, Set<Location>> entry = expirationBuckets.firstEntry();
            if (entry.getKey() > now) {
                break;
            }

            expirationBuckets.pollFirstEntry();
            for (Location location : new ArrayList<>(entry.getValue())) {
                Long expectedExpireAt = blockExpireAt.get(location);
                if (expectedExpireAt == null || expectedExpireAt.longValue() != entry.getKey()) {
                    continue;
                }

                Block block = location.getBlock();
                BlockData expectedData = placedBlocks.get(location);
                if (expectedData == null || block.getType() != expectedData.getMaterial()) {
                    TempBuild.getInstance().replacedPlantManager.removeWithoutRestore(location);
                    clearBlock(location);
                    continue;
                }

                removeBlock(location, block);
            }
        }
    }

    private static void updateDamageAnimations() {
        if (placedBlocks.size() > MAX_ANIMATION_BLOCKS) {
            return;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<Location, Long> entry : blockPlaceTime.entrySet()) {
            Location location = entry.getKey();
            Long placeTime = entry.getValue();
            Long decayTimeMs = blockDecayDuration.get(location);
            Integer entityId = blockEntityIds.get(location);
            BlockData expectedData = placedBlocks.get(location);

            if (placeTime == null || decayTimeMs == null || entityId == null || expectedData == null) {
                continue;
            }

            float progress = Math.min(1f, Math.max(0f, (float) (now - placeTime) / decayTimeMs));
            for (Player player : location.getWorld().getPlayers()) {
                player.sendBlockDamage(location, progress, entityId);
            }
        }
    }

    private static void unregisterExpiration(Location location) {
        Long expireAt = blockExpireAt.remove(location);
        if (expireAt == null) {
            return;
        }

        Set<Location> bucket = expirationBuckets.get(expireAt);
        if (bucket == null) {
            return;
        }

        bucket.remove(location);
        if (bucket.isEmpty()) {
            expirationBuckets.remove(expireAt);
        }
    }

    private static void clearBlock(Location location) {
        unregisterExpiration(location);

        if (TempBuild.getInstance().tempBlockStorage != null) {
            TempBuild.getInstance().tempBlockStorage.delete(location);
        }

        Integer entityId = blockEntityIds.remove(location);
        if (entityId != null) {
            for (Player player : location.getWorld().getPlayers()) {
                player.sendBlockDamage(location, 0f, entityId);
            }
        }

        placedBlocks.remove(location);
        blockPlaceTime.remove(location);
        blockDecayDuration.remove(location);
    }

    private static void removeBlock(Location location, Block block) {
        BlockData data = placedBlocks.get(location);
        clearBlock(location);

        if (TempBuild.getInstance().config.dropBlocks) {
            block.breakNaturally();
        }

        block.setType(Material.AIR);
        TempBuild.getInstance().replacedPlantManager.restoreAndForget(location);

        if (data instanceof Bisected bisected && bisected.getHalf() == Bisected.Half.BOTTOM) {
            Location topLocation = location.clone().add(0, 1, 0);
            clearBlock(topLocation);
            topLocation.getBlock().setType(Material.AIR);
            TempBuild.getInstance().replacedPlantManager.restoreAndForget(topLocation);
        }
    }
}
