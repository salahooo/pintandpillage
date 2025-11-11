package nl.duckstudios.pintandpillage.helper;

import nl.duckstudios.pintandpillage.entity.Village;
import nl.duckstudios.pintandpillage.entity.buildings.Building;

/**
 * Utility to register fully constructed buildings for deterministic testing.
 */
public final class BuildingEffectApplier {

    private BuildingEffectApplier() {
    }

    public static void registerBuiltBuilding(Village village, Building building, int level) {
        building.setLevel(level);
        building.setUnderConstruction(false);
        building.setVillage(village);
        building.updateBuilding();
        village.createBuilding(building); // REFACTOR (ITSTEN H2): Centralized effect application to assert state changes.
    }
}

