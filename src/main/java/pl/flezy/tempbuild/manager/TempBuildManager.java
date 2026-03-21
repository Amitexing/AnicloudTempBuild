package pl.flezy.tempbuild.manager;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Player;
import pl.flezy.tempbuild.TempBuild;

import java.util.*;

public class TempBuildManager {
    public static boolean hasBypass(Player player, Location location) {
        World world = location.getWorld();
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        return WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(world));
    }

    public static boolean isDenied(Player player, Location location) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        StateFlag tempBuildFlag = TempBuild.getInstance().TEMP_BUILD_FLAG;

        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();

        StateFlag.State state = regionContainer.createQuery().queryState(BukkitAdapter.adapt(location), localPlayer, tempBuildFlag);
        return state != StateFlag.State.ALLOW;
    }

    public static boolean isRegion(Location location) {
        RegionContainer container = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer();

        StateFlag tempBuildFlag = TempBuild.getInstance().TEMP_BUILD_FLAG;

        StateFlag.State state = container.createQuery()
                .queryState(BukkitAdapter.adapt(location), null, tempBuildFlag);

        return state == StateFlag.State.ALLOW;
    }

    public static boolean canBreak(Player player, Block block) {
        Location location = block.getLocation();

        if (isTempBuildBlock(location)) {
            return true;
        }

        if (TempBuild.getInstance().ultimateBlockRegenHook.canBreak(block)) {
            return true;
        }

        return true;
    }


    public static void updateBlockData(Location location) {
        if (isTempBuildBlock(location)) {
            BlockDecayManager.placedBlocks.put(location, location.getBlock().getBlockData());
        }
    }

    public static boolean isTempBuildBlock(Location location) {
        return BlockDecayManager.placedBlocks.containsKey(location);
    }

    public static boolean isDoor(Material material) {
        return Tag.DOORS.isTagged(material);
    }

    public static boolean isInteractiveDoorLike(Material material) {
        return Tag.DOORS.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material)
                || Tag.BUTTONS.isTagged(material)
                || Tag.PRESSURE_PLATES.isTagged(material)
                || material == Material.LEVER;
    }

    public static boolean isLiquid(Material material) {
        return List.of(
                Material.WATER,
                Material.LAVA
        ).contains(material);
    }
}
