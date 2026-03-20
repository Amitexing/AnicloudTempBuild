package pl.flezy.tempbuild;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.flezy.tempbuild.command.TempBuildCommand;
import pl.flezy.tempbuild.config.Config;
import pl.flezy.tempbuild.listener.BuildListener;
import pl.flezy.tempbuild.listener.FireListener;
import pl.flezy.tempbuild.listener.RegionInteractionBlockListener;
import pl.flezy.tempbuild.listener.RegionInteractionEntityListener;
import pl.flezy.tempbuild.manager.BlockDecayManager;
import pl.flezy.tempbuild.manager.ConfigurationManager;
import pl.flezy.tempbuild.manager.RegionFlagManager;
import pl.flezy.tempbuild.manager.TempBlockStorage;
import pl.flezy.tempbuild.manager.UltimateBlockRegenHook;

import java.util.ArrayList;

public final class TempBuild extends JavaPlugin {

    private static TempBuild instance;
    public Config config;
    public StateFlag TEMP_BUILD_FLAG;
    public StateFlag OC_INTERACTION_LOCK_FLAG;
    public StateFlag CAMPFIRE_INTERACTION_LOCK_FLAG;
    public StateFlag TRAPDOOR_LOCK_FLAG;
    public UltimateBlockRegenHook ultimateBlockRegenHook;
    public TempBlockStorage tempBlockStorage;
    public RegionFlagManager regionFlagManager;

    @Override
    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        TEMP_BUILD_FLAG = registerStateFlag(registry, "temp-build");
        OC_INTERACTION_LOCK_FLAG = registerStateFlag(registry, "oc-interaction-lock");
        CAMPFIRE_INTERACTION_LOCK_FLAG = registerStateFlag(registry, "campfire-interaction-lock");
        TRAPDOOR_LOCK_FLAG = registerStateFlag(registry, "trapdoor-lock");
    }

    private StateFlag registerStateFlag(FlagRegistry registry, String name) {
        try {
            StateFlag flag = new StateFlag(name, false);
            registry.register(flag);
            return flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag) {
                return (StateFlag) existing;
            }

            getLogger().warning("Flag '" + name + "' already exists and is not a StateFlag!");
            return null;
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuard not found. Disabling AnicloudTempBuild.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        config = ConfigurationManager.getConfig(this);
        config.ensureBlockDecayTimesFilled();
        config.save();
        tempBlockStorage = new TempBlockStorage(this);
        tempBlockStorage.initialize();
        regionFlagManager = new RegionFlagManager();
        ultimateBlockRegenHook = new UltimateBlockRegenHook(getServer().getPluginManager());

        if (ultimateBlockRegenHook.isHooked()) {
            getLogger().info("Detected UltimateBlockRegen: compatibility mode enabled.");
        }

        getServer().getPluginManager().registerEvents(new RegionInteractionBlockListener(), this);
        getServer().getPluginManager().registerEvents(new RegionInteractionEntityListener(), this);
        getServer().getPluginManager().registerEvents(new BuildListener(), this);
        getServer().getPluginManager().registerEvents(new FireListener(), this);

        getCommand("tempbuild").setExecutor(new TempBuildCommand());
        getCommand("tempbuild").setTabCompleter(new TempBuildCommand());

        BlockDecayManager.initialize();
        BlockDecayManager.loadPersistedBlocks();
    }

    @Override
    public void onDisable() {
        if (tempBlockStorage != null) {
            tempBlockStorage.close();
        }
    }

    public static TempBuild getInstance() {
        return instance;
    }

    public void reload() {
        this.config.blockedBlocks = new ArrayList<>();
        this.config.load();
        this.config.ensureBlockDecayTimesFilled();
        this.config.save();
    }
}
