package pl.flezy.tempbuild.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.flezy.tempbuild.TempBuild;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class PotLootManager {
    private final TempBuild plugin;
    private final File file;
    private final List<LootEntry> entries = new ArrayList<>();

    public PotLootManager(TempBuild plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "potloot.yml");
    }

    public void load() {
        entries.clear();
        ensureFileExists();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            double chance = parseChance(section.getString("CHANCE", "0"));
            if (chance <= 0d) {
                plugin.getLogger().warning("potloot.yml: section '" + key + "' has non-positive CHANCE, skipped.");
                continue;
            }

            List<RewardAction> actions = new ArrayList<>();
            for (String rawReward : section.getStringList("REWARDS")) {
                RewardAction action = parseReward(rawReward, key);
                if (action != null) {
                    actions.add(action);
                }
            }

            if (!actions.isEmpty()) {
                entries.add(new LootEntry(chance, actions));
            }
        }
    }

    public void applyRandomLoot(Player player, Location blockLocation) {
        LootEntry entry = chooseOneEntry();
        if (entry == null) {
            return;
        }

        for (RewardAction action : entry.actions()) {
            action.apply(player, blockLocation);
        }
    }

    private LootEntry chooseOneEntry() {
        if (entries.isEmpty()) {
            return null;
        }

        double total = 0d;
        for (LootEntry entry : entries) {
            total += entry.chance();
        }

        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0d;
        for (LootEntry entry : entries) {
            cumulative += entry.chance();
            if (roll <= cumulative) {
                return entry;
            }
        }

        return entries.get(entries.size() - 1);
    }

    private RewardAction parseReward(String rawReward, String sectionKey) {
        String[] split = rawReward.split("\\s*:\\s*", 3);
        if (split.length < 2) {
            plugin.getLogger().warning("potloot.yml: invalid reward format in section '" + sectionKey + "': " + rawReward);
            return null;
        }

        String type = split[0].trim().toUpperCase(Locale.ROOT);
        if (type.equals("ITEM")) {
            String[] itemSplit = rawReward.split("\\s*:\\s*");
            if (itemSplit.length < 3) {
                plugin.getLogger().warning("potloot.yml: invalid ITEM reward in section '" + sectionKey + "': " + rawReward);
                return null;
            }

            Material material = Material.matchMaterial(itemSplit[1].trim());
            if (material == null) {
                plugin.getLogger().warning("potloot.yml: unknown material in section '" + sectionKey + "': " + itemSplit[1]);
                return null;
            }

            int amount;
            try {
                amount = Integer.parseInt(itemSplit[2].trim());
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("potloot.yml: invalid ITEM amount in section '" + sectionKey + "': " + rawReward);
                return null;
            }

            return (player, blockLocation) -> blockLocation.getWorld().dropItemNaturally(blockLocation, new ItemStack(material, amount));
        }

        if (type.equals("CONSOLE")) {
            String command = split[1].trim();
            if (split.length == 3) {
                command = split[1].trim() + " : " + split[2].trim();
            }

            final String finalCommand = command;
            return (player, blockLocation) -> Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    applyPlaceholders(finalCommand, player, blockLocation)
            );
        }

        plugin.getLogger().warning("potloot.yml: unknown reward type in section '" + sectionKey + "': " + rawReward);
        return null;
    }

    private String applyPlaceholders(String command, Player player, Location location) {
        String blockLocation = String.format(
                Locale.US,
                "%.1f %.1f %.1f",
                location.getX() + 0.5,
                location.getY() + 0.5,
                location.getZ() + 0.5
        );

        return command
                .replace("%player%", player.getName())
                .replace("%world%", location.getWorld().getName())
                .replace("%block_location%", blockLocation);
    }

    private double parseChance(String rawChance) {
        String normalized = rawChance.replace("%", "").trim();
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException exception) {
            return 0d;
        }
    }

    private void ensureFileExists() {
        if (file.exists()) {
            return;
        }

        try {
            plugin.saveResource("potloot.yml", false);
        } catch (IllegalArgumentException ignored) {
            try {
                if (file.createNewFile()) {
                    YamlConfiguration template = new YamlConfiguration();
                    template.set("1.CHANCE", "10%");
                    template.set("1.REWARDS", List.of("ITEM : DIAMOND : 1"));
                    template.save(file);
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Unable to create potloot.yml: " + exception.getMessage());
            }
        }
    }

    private record LootEntry(double chance, List<RewardAction> actions) {}

    @FunctionalInterface
    private interface RewardAction {
        void apply(Player player, Location blockLocation);
    }
}
