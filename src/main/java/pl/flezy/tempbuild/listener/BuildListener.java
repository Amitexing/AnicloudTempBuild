package pl.flezy.tempbuild.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
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
import pl.flezy.tempbuild.TempBuild;
import pl.flezy.tempbuild.config.Config;
import pl.flezy.tempbuild.manager.BlockDecayManager;
import pl.flezy.tempbuild.manager.TempBuildManager;

public class BuildListener implements Listener {
    private static final int ULTIMATE_BLOCK_REGEN_CLEANUP_TICKS = 60;

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (TempBuildManager.hasBypass(player, location)) return;

        if (TempBuildManager.isDenied(player, location) && !player.hasPermission("anicloud.tempbuildbypass")) {
            event.setCancelled(true);
            return;
        }

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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();
        if (TempBuildManager.hasBypass(player, location)) return;

        if (TempBuildManager.isTempBuildBlock(location)) {
            event.setCancelled(true);
            breakTempBuildBlock(player, block, location);
            return;
        }

        if (!TempBuildManager.canBreak(player, block)){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreakFinal(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (TempBuildManager.isTempBuildBlock(location)) {
            event.setCancelled(true);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (handleDoorInteraction(event, block)) {
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

        if (TempBuildManager.isDoor(block.getType())) {
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

    private void breakTempBuildBlock(Player player, Block block, Location location) {
        if (block.getBlockData() instanceof Bisected bisected) {
            Location bottomLocation = bisected.getHalf() == Bisected.Half.TOP
                    ? location.clone().add(0, -1, 0)
                    : location.clone();
            Location topLocation = bottomLocation.clone().add(0, 1, 0);

            BlockDecayManager.untrackBlock(bottomLocation);
            BlockDecayManager.untrackBlock(topLocation);

            clearBlockForTempBuildBreak(player, bottomLocation.getBlock());
            topLocation.getBlock().setType(Material.AIR, false);

            scheduleUltimateBlockRegenDelayBlockCleanup(bottomLocation);
            scheduleUltimateBlockRegenDelayBlockCleanup(topLocation);
            return;
        }

        BlockDecayManager.untrackBlock(location);
        clearBlockForTempBuildBreak(player, block);
        scheduleUltimateBlockRegenDelayBlockCleanup(location);
    }

    private void clearBlockForTempBuildBreak(Player player, Block targetBlock) {
        if (targetBlock.getType().isAir()) {
            return;
        }

        if (!player.getGameMode().name().equals("CREATIVE")) {
            targetBlock.breakNaturally(player.getInventory().getItemInMainHand());
        }

        if (!targetBlock.getType().isAir()) {
            targetBlock.setType(Material.AIR, false);
        }
    }

    private void scheduleUltimateBlockRegenDelayBlockCleanup(Location location) {
        if (!TempBuild.getInstance().ultimateBlockRegenHook.isHooked()) {
            return;
        }

        final int[] livedTicks = {0};
        Bukkit.getScheduler().runTaskTimer(TempBuild.getInstance(), task -> {
            livedTicks[0]++;
            if (livedTicks[0] > ULTIMATE_BLOCK_REGEN_CLEANUP_TICKS) {
                task.cancel();
                return;
            }

            Block currentBlock = location.getBlock();
            if (TempBuildManager.isTempBuildBlock(location)) {
                task.cancel();
                return;
            }

            if (currentBlock.getType() == Material.BEDROCK) {
                currentBlock.setType(Material.AIR, false);
            } else {
                task.cancel();
            }
        }, 1L, 1L);
    }

    private boolean handleDoorInteraction(PlayerInteractEvent event, Block clickedBlock) {
        if (!TempBuildManager.isDoor(clickedBlock.getType())) {
            return false;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return true;
        }

        Location clickedLocation = clickedBlock.getLocation();
        Location bottomLocation = clickedLocation;
        if (clickedBlock.getBlockData() instanceof Bisected bisected && bisected.getHalf() == Bisected.Half.TOP) {
            bottomLocation = clickedLocation.clone().add(0, -1, 0);
        }

        Location topLocation = bottomLocation.clone().add(0, 1, 0);
        Block bottom = bottomLocation.getBlock();
        Block top = topLocation.getBlock();
        if (!(bottom.getBlockData() instanceof Door bottomDoor) || !(top.getBlockData() instanceof Door topDoor)) {
            return false;
        }

        boolean newOpenState = !bottomDoor.isOpen();
        bottomDoor.setOpen(newOpenState);
        topDoor.setOpen(newOpenState);
        bottom.setBlockData(bottomDoor, false);
        top.setBlockData(topDoor, false);

        TempBuildManager.updateBlockData(bottomLocation);
        TempBuildManager.updateBlockData(topLocation);

        event.setCancelled(true);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        return true;
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
