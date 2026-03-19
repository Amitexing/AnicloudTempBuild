package pl.flezy.tempbuild.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.flezy.tempbuild.TempBuild;
import pl.flezy.tempbuild.config.Config;
import pl.flezy.tempbuild.manager.BlockDecayManager;
import pl.flezy.tempbuild.manager.TempBuildManager;

public class BuildListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (TempBuildManager.hasBypass(player, location)) return;

        if (!TempBuildManager.isDenied(player, location)) {
            Config config = TempBuild.getInstance().config;

            Block block = event.getBlock();
            if (config.blockedBlocks.contains(block.getType())) {
                event.setCancelled(true);
                return;
            }

            if (!BlockDecayManager.placedBlocks.containsKey(location)) {
                BlockState replacedBlockState = event.getBlockReplacedState();
                if (replacedBlockState.isCollidable()) {
                    event.setCancelled(true);
                    return;
                }

                if (!replacedBlockState.isCollidable() &&
                        !TempBuildManager.isLiquid(replacedBlockState.getType()) &&
                        !replacedBlockState.getType().isEmpty() &&
                        !config.allowReplaceNonCollidableBlocks) {
                    event.setCancelled(true);
                    return;
                }

                if (TempBuildManager.isLiquid(replacedBlockState.getType()) &&
                        !config.allowReplaceLiquids) {
                    event.setCancelled(true);
                    return;
                }
            }

            BlockDecayManager.addPlayerBlock(location);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();
        if (TempBuildManager.hasBypass(player, location)) return;

        if (TempBuildManager.isTempBuildBlock(location) && event.isCancelled()) {
            event.setCancelled(false);
        }

        if (!TempBuildManager.canBreak(player, block)){
            event.setCancelled(true);
        }
        else if (TempBuildManager.isTempBuildBlock(location)) {
            if (block.getBlockData() instanceof Bisected bisected) {
                if (bisected.getHalf() == Bisected.Half.TOP) {
                    Location bottomLocation = location.clone().add(0, -1, 0);
                    BlockDecayManager.placedBlocks.remove(bottomLocation);
                }
            }
            BlockDecayManager.placedBlocks.remove(location);

            Bukkit.getScheduler().runTask(TempBuild.getInstance(), () -> {
                Block currentBlock = location.getBlock();
                if (currentBlock.getType() == Material.BEDROCK && !TempBuildManager.isTempBuildBlock(location)) {
                    currentBlock.setType(Material.AIR);
                }
            });
        }
    }

    @EventHandler
    public void onBlockExplodeEvent(BlockExplodeEvent event) {
        if (TempBuild.getInstance().config.protectFromExplosions) {
            event.blockList().removeIf(block -> TempBuildManager.isRegion(block.getLocation()));
        }
    }

    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        if (TempBuild.getInstance().config.protectFromExplosions) {
            event.blockList().removeIf(block -> TempBuildManager.isRegion(block.getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                TempBuild.getInstance().ultimateBlockRegenHook.isHooked() &&
                TempBuildManager.isDoor(block.getType())) {
            event.setCancelled(false);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);
        }

        if (TempBuildManager.isTempBuildBlock(block.getLocation())) {
            TempBuildManager.updateBlockData(block.getLocation());

            Location topLocation = block.getLocation().clone().add(0, 1, 0);
            if (TempBuildManager.isTempBuildBlock(topLocation)) {
                TempBuildManager.updateBlockData(topLocation);
            }

            Location bottomLocation = block.getLocation().clone().add(0, -1, 0);
            if (TempBuildManager.isTempBuildBlock(bottomLocation)) {
                TempBuildManager.updateBlockData(bottomLocation);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        Location toLocation = event.getToBlock().getLocation();
        if (TempBuildManager.isRegion(toLocation) &&
                !TempBuild.getInstance().config.allowLiquidFlow) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (BlockDecayManager.placedBlocks.containsKey(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Location location = event.getBlock().getLocation();
        if (BlockDecayManager.placedBlocks.containsKey(location)) {
            TempBuildManager.updateBlockData(location);
        }
    }

    @EventHandler
    public void onEmptyBucket(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();
        if (!TempBuildManager.isDenied(player, location)) {
            Material insideBucketMaterial;
            switch (event.getBucket()) {
                case LAVA_BUCKET -> insideBucketMaterial = Material.LAVA;
                case WATER_BUCKET -> insideBucketMaterial = Material.WATER;
                case POWDER_SNOW -> insideBucketMaterial = Material.POWDER_SNOW;
                default -> {
                    return;
                }
            }

            if (TempBuild.getInstance().config.blockedBlocks.contains(insideBucketMaterial)) {
                event.setCancelled(true);
                return;
            }

            if (block.isEmpty()) {
                BlockDecayManager.addPlayerBlock(location);
            }
        }
    }
}
