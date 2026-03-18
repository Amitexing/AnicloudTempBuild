package pl.flezy.tempbuild.manager;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import pl.flezy.tempbuild.TempBuild;

public class UltimateBlockRegenHook {
    private final boolean hooked;

    public UltimateBlockRegenHook(PluginManager pluginManager) {
        Plugin plugin = pluginManager.getPlugin("UltimateBlockRegen");
        this.hooked = plugin != null && plugin.isEnabled();
    }

    public boolean isHooked() {
        return hooked;
    }

    public boolean canBreak(Block block) {
        if (!hooked) {
            return false;
        }

        return TempBuild.getInstance().config.allowBreakUltimateBlockRegenBlocks
                && block.getType() == Material.BEDROCK;
    }
}
