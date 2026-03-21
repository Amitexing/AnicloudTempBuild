package pl.flezy.tempbuild.listener;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import pl.flezy.tempbuild.TempBuild;

public class RegionInteractionEntityListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (shouldLockEntity(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (shouldLockEntity(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player player = resolvePlayerDamager(event.getDamager());
        if (player == null) {
            return;
        }

        if (shouldLockEntity(player, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) {
            return;
        }

        if (shouldLockEntity(player, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private boolean shouldLockEntity(Player player, Entity entity) {
        if (player == null || player.isOp()) {
            return false;
        }

        if (!(entity instanceof ItemFrame || entity instanceof GlowItemFrame || entity instanceof ArmorStand)) {
            return false;
        }

        TempBuild plugin = TempBuild.getInstance();
        return plugin.regionFlagManager.isLocked(player, entity.getLocation(), plugin.OC_INTERACTION_LOCK_FLAG);
    }

    private Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }

        return null;
    }
}
