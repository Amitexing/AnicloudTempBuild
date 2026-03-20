package pl.flezy.tempbuild.listener;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.flezy.tempbuild.TempBuild;

import java.util.Set;

public class RegionInteractionBlockListener implements Listener {
    private static final Set<Material> LOCKED_BLOCKS = Set.of(
            Material.DECORATED_POT,
            Material.CHISELED_BOOKSHELF,
            Material.LECTERN,
            Material.JUKEBOX,
            Material.CRAFTER
    );

    private static final Set<Material> CAMPFIRE_BLOCKS = Set.of(
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        Player player = event.getPlayer();

        if (clicked == null || player.isOp()) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK &&
                event.getAction() != Action.LEFT_CLICK_BLOCK &&
                event.getAction() != Action.PHYSICAL) {
            return;
        }

        Material type = clicked.getType();
        TempBuild plugin = TempBuild.getInstance();

        boolean lockedByMainFlag = LOCKED_BLOCKS.contains(type) &&
                plugin.regionFlagManager.isLocked(player, clicked.getLocation(), plugin.OC_INTERACTION_LOCK_FLAG);

        boolean lockedByCampfireFlag = CAMPFIRE_BLOCKS.contains(type) &&
                plugin.regionFlagManager.isLocked(player, clicked.getLocation(), plugin.CAMPFIRE_INTERACTION_LOCK_FLAG);

        boolean lockedByTrapdoorFlag = Tag.TRAPDOORS.isTagged(type) &&
                plugin.regionFlagManager.isLocked(player, clicked.getLocation(), plugin.TRAPDOOR_LOCK_FLAG);

        boolean lockedSign = Tag.ALL_SIGNS.isTagged(type) &&
                plugin.regionFlagManager.isLocked(player, clicked.getLocation(), plugin.OC_INTERACTION_LOCK_FLAG);

        if (!lockedByMainFlag && !lockedByCampfireFlag && !lockedByTrapdoorFlag && !lockedSign) {
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.isOp()) {
            return;
        }

        TempBuild plugin = TempBuild.getInstance();
        if (plugin.regionFlagManager.isLocked(player, event.getBlock().getLocation(), plugin.OC_INTERACTION_LOCK_FLAG)) {
            event.setCancelled(true);
        }
    }
}
