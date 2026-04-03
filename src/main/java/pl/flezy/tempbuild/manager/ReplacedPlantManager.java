package pl.flezy.tempbuild.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import pl.flezy.tempbuild.TempBuild;

import java.util.*;

public class ReplacedPlantManager {
    private final Map<Location, Map<Location, BlockData>> replacedPlantsByTempBlock = new HashMap<>();

    public void loadPersisted() {
        TempBlockStorage storage = TempBuild.getInstance().tempBlockStorage;
        if (storage == null) {
            return;
        }

        replacedPlantsByTempBlock.clear();
        for (TempBlockStorage.StoredReplacedPlant row : storage.loadAllReplacedPlants()) {
            replacedPlantsByTempBlock
                    .computeIfAbsent(row.tempLocation(), ignored -> new HashMap<>())
                    .put(row.partLocation(), row.partData());
        }
    }

    public Map<Location, BlockData> captureReplacedPlant(Location placedLocation, BlockData replacedData) {
        BlockData currentData = replacedData;
        if (!ReplaceablePlantHelper.isReplaceablePlant(currentData.getMaterial())) {
            return Collections.emptyMap();
        }

        Map<Location, BlockData> result = new HashMap<>();
        result.put(placedLocation, currentData.clone());

        if (currentData instanceof Bisected bisected && ReplaceablePlantHelper.isDoublePlant(currentData)) {
            Location secondaryLocation = bisected.getHalf() == Bisected.Half.BOTTOM
                    ? placedLocation.clone().add(0, 1, 0)
                    : placedLocation.clone().add(0, -1, 0);

            Block secondaryBlock = secondaryLocation.getBlock();
            if (secondaryBlock.getBlockData() instanceof Bisected secondaryData
                    && secondaryData.getMaterial() == currentData.getMaterial()) {
                result.put(secondaryLocation, secondaryData.clone());
            }
        }

        return result;
    }

    public void saveReplacement(Location tempBlockLocation, Map<Location, BlockData> replacedPlantParts) {
        if (replacedPlantParts.isEmpty()) {
            return;
        }

        replacedPlantsByTempBlock.put(tempBlockLocation, new HashMap<>(replacedPlantParts));
        if (TempBuild.getInstance().tempBlockStorage != null) {
            TempBuild.getInstance().tempBlockStorage.saveReplacedPlant(tempBlockLocation, replacedPlantParts);
        }
    }

    public void restoreAndForget(Location tempBlockLocation) {
        Map<Location, BlockData> parts = replacedPlantsByTempBlock.remove(tempBlockLocation);
        if (parts == null || parts.isEmpty()) {
            if (TempBuild.getInstance().tempBlockStorage != null) {
                TempBuild.getInstance().tempBlockStorage.deleteReplacedPlant(tempBlockLocation);
            }
            return;
        }

        boolean canRestore = true;
        for (Location location : parts.keySet()) {
            Block block = location.getBlock();
            if (!block.getType().isAir()) {
                canRestore = false;
                break;
            }
        }

        if (canRestore) {
            for (Map.Entry<Location, BlockData> part : parts.entrySet()) {
                part.getKey().getBlock().setBlockData(part.getValue(), false);
            }
        }

        if (TempBuild.getInstance().tempBlockStorage != null) {
            TempBuild.getInstance().tempBlockStorage.deleteReplacedPlant(tempBlockLocation);
        }
    }

    public void removeWithoutRestore(Location tempBlockLocation) {
        replacedPlantsByTempBlock.remove(tempBlockLocation);
        if (TempBuild.getInstance().tempBlockStorage != null) {
            TempBuild.getInstance().tempBlockStorage.deleteReplacedPlant(tempBlockLocation);
        }
    }
}
