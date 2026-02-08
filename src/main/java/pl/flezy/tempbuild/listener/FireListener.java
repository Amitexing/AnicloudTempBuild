package pl.flezy.tempbuild.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import pl.flezy.tempbuild.TempBuild;
import pl.flezy.tempbuild.manager.TempBuildManager;

public class FireListener implements Listener {

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (TempBuildManager.isRegion(event.getBlock().getLocation()) &&
                !TempBuild.getInstance().config.allowBlockSpread) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (TempBuildManager.isRegion(event.getBlock().getLocation()) &&
                !TempBuild.getInstance().config.allowFireIgnite) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (TempBuildManager.isRegion(event.getBlock().getLocation()) &&
                !TempBuild.getInstance().config.allowFireBurn) {
            event.setCancelled(true);
        }
    }
}
