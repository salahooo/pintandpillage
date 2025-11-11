package nl.duckstudios.pintandpillage.helper;

import nl.duckstudios.pintandpillage.entity.Coord;
import nl.duckstudios.pintandpillage.model.BuildPosition;

import java.util.Arrays;

public final class PlacementRules {

    private PlacementRules() {
    }

    public static boolean isPlacementAllowed(BuildPosition[] positions, String buildingName, Coord position) {
        return Arrays.stream(positions)
                .filter(bp -> bp.position.getX() == position.getX() && bp.position.getY() == position.getY())
                .anyMatch(bp -> bp.allowedBuilding == null || bp.allowedBuilding.equals(buildingName)); // REFACTOR (ITSTEN H2): Extracted pure placement rule for testability.
    }
}
