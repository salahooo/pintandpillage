package nl.duckstudios.pintandpillage.helper;

import nl.duckstudios.pintandpillage.entity.Coord;
import nl.duckstudios.pintandpillage.model.BuildPosition;

import java.util.Arrays;

public final class PlacementRules {

    private PlacementRules() {
    }

    public static boolean isPlacementAllowed(BuildPosition[] positions, String buildingName, Coord position) {
        boolean hasDedicatedPlacement = Arrays.stream(positions)
                .anyMatch(bp -> buildingName.equals(bp.allowedBuilding));

        return Arrays.stream(positions)
                .filter(bp -> bp.position.getX() == position.getX() && bp.position.getY() == position.getY())
                .anyMatch(bp -> buildingName.equals(bp.allowedBuilding)
                        || (bp.allowedBuilding == null && !hasDedicatedPlacement)); // REFACTOR (ITSTEN H2): Extracted pure placement rule for testability.
    }
}
