package pl.flezy.tempbuild.manager;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.validator.okaeri.OkaeriValidator;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import pl.flezy.tempbuild.TempBuild;
import pl.flezy.tempbuild.config.Config;

import java.io.File;

public class ConfigurationManager {
    public static Config getConfig(TempBuild plugin) {
        return (Config) ConfigManager.create(Config.class)
                .withConfigurer(new OkaeriValidator(new YamlBukkitConfigurer()))
                .withSerdesPack(registry -> {
                    registry.register(new SerdesCommons());
                    registry.register(new SerdesBukkit());
                })
                .withBindFile(new File(plugin.getDataFolder(), "config.yml"))
                .saveDefaults()
                .load(true);
    }
}
