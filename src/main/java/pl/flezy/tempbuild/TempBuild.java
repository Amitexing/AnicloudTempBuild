package pl.flezy.tempbuild;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.plugin.java.JavaPlugin;
import pl.flezy.tempbuild.command.TempBuildCommand;
import pl.flezy.tempbuild.config.Config;
import pl.flezy.tempbuild.listener.BuildListener;
import pl.flezy.tempbuild.listener.FireListener;
import pl.flezy.tempbuild.manager.BlockDecayManager;
import pl.flezy.tempbuild.manager.ConfigurationManager;
import pl.flezy.tempbuild.manager.TempBlockStorage;
import pl.flezy.tempbuild.manager.UltimateBlockRegenHook;

import java.util.ArrayList;

public final class TempBuild extends JavaPlugin {

    private static TempBuild instance;
    public Config config;
    public StateFlag TEMP_BUILD_FLAG;
    public UltimateBlockRegenHook ultimateBlockRegenHook;
    public TempBlockStorage tempBlockStorage;

    @Override
    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("temp-build", false);
            registry.register(flag);
            TEMP_BUILD_FLAG = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("temp-build");
            if (existing instanceof StateFlag) {
                TEMP_BUILD_FLAG = (StateFlag) existing;
            } else {
                getLogger().warning("Flag 'temp-build' already exists and is not a StateFlag!");
            }
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        config = ConfigurationManager.getConfig(this);
        config.ensureBlockDecayTimesFilled();
        config.save();
        tempBlockStorage = new TempBlockStorage(this);
        tempBlockStorage.initialize();
        ultimateBlockRegenHook = new UltimateBlockRegenHook(getServer().getPluginManager());

        if (ultimateBlockRegenHook.isHooked()) {
            getLogger().info("Detected UltimateBlockRegen: compatibility mode enabled.");
        }

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
