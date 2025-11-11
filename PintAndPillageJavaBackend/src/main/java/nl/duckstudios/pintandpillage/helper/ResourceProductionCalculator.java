package nl.duckstudios.pintandpillage.helper;

import nl.duckstudios.pintandpillage.model.ResourceType;

/**
 * Pure calculator that determines hourly resource production per building level.
 */
public class ResourceProductionCalculator {

    public static final int DEFAULT_MAX_LEVEL = 30;

    public int productionPerHour(ResourceType type, int level) {
        if (level <= 0) {
            return 0;
        }

        double value = switch (type) {
            case Beer -> 10 + 6 * Math.pow(level, 1.15);
            case Wood, Stone, Hop -> 20 + 12 * Math.pow(level, 1.2);
            default -> 0;
        };

        return (int) value;
    }
}
