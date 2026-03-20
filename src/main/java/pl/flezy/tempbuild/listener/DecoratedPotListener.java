package pl.flezy.tempbuild.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import pl.flezy.tempbuild.TempBuild;

public class DecoratedPotListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDecoratedPotBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DECORATED_POT) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }

        TempBuild plugin = TempBuild.getInstance();
        boolean locked = plugin.regionFlagManager.isLocked(player, block.getLocation(), plugin.OC_INTERACTION_LOCK_FLAG);
        if (!locked) {
            return;
        }

        event.setCancelled(true);
        block.setType(Material.AIR, false);
        plugin.potLootManager.applyRandomLoot(player, block.getLocation());
    }
}
