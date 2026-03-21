package pl.flezy.tempbuild.manager;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RegionFlagManager {
    public boolean isLocked(Player player, Location location, StateFlag flag) {
        if (flag == null || player == null || location == null) {
            return false;
        }

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        StateFlag.State state = regionContainer
                .createQuery()
                .queryState(BukkitAdapter.adapt(location), localPlayer, flag);

        return state == StateFlag.State.ALLOW;
    }
}
