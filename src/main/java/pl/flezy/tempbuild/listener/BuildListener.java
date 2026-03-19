package pl.flezy.tempbuild.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;
import pl.flezy.tempbuild.TempBuild;
import pl.flezy.tempbuild.config.Config;
import pl.flezy.tempbuild.manager.BlockDecayManager;
import pl.flezy.tempbuild.manager.TempBuildManager;

public class BuildListener implements Listener {
    private static final int ULTIMATE_BLOCK_REGEN_CLEANUP_TICKS = 40;

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
            event.setDropItems(true);
        }

        if (!TempBuildManager.canBreak(player, block)){
            event.setCancelled(true);
        }
        else if (TempBuildManager.isTempBuildBlock(location)) {
            if (block.getBlockData() instanceof Bisected bisected) {
                if (bisected.getHalf() == Bisected.Half.TOP) {
                    Location bottomLocation = location.clone().add(0, -1, 0);
                    BlockDecayManager.placedBlocks.remove(bottomLocation);
                    scheduleUltimateBlockRegenDelayBlockCleanup(bottomLocation);
                } else if (bisected.getHalf() == Bisected.Half.BOTTOM) {
                    Location topLocation = location.clone().add(0, 1, 0);
                    BlockDecayManager.placedBlocks.remove(topLocation);
                    scheduleUltimateBlockRegenDelayBlockCleanup(topLocation);
                }
            }
            BlockDecayManager.placedBlocks.remove(location);
            scheduleUltimateBlockRegenDelayBlockCleanup(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreakFinal(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (!TempBuildManager.isTempBuildBlock(location)) {
            return;
        }

        if (event.isCancelled()) {
            event.setCancelled(false);
        }
        event.setDropItems(true);
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

        allowInteractionAgainstUltimateBlockRegen(event, block);

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerInteractFinal(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        allowInteractionAgainstUltimateBlockRegen(event, block);
    }

    private void allowInteractionAgainstUltimateBlockRegen(PlayerInteractEvent event, Block block) {
        if (!TempBuild.getInstance().ultimateBlockRegenHook.isHooked()) {
            return;
        }

        if (!TempBuildManager.isInteractiveDoorLike(block.getType())) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK &&
                event.getAction() != Action.LEFT_CLICK_BLOCK &&
                event.getAction() != Action.PHYSICAL) {
            return;
        }

        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        event.setCancelled(false);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);
    }

    private void scheduleUltimateBlockRegenDelayBlockCleanup(Location location) {
        if (!TempBuild.getInstance().ultimateBlockRegenHook.isHooked()) {
            return;
        }

        BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(TempBuild.getInstance(), () -> {
            Block currentBlock = location.getBlock();
            if (TempBuildManager.isTempBuildBlock(location)) {
                taskRef[0].cancel();
                return;
            }

            if (currentBlock.getType() == Material.BEDROCK) {
                currentBlock.setType(Material.AIR);
            } else {
                taskRef[0].cancel();
            }
        }, 1L, 1L);

        Bukkit.getScheduler().runTaskLater(TempBuild.getInstance(), () -> {
            if (taskRef[0] != null) {
                taskRef[0].cancel();
            }
        }, ULTIMATE_BLOCK_REGEN_CLEANUP_TICKS);
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
