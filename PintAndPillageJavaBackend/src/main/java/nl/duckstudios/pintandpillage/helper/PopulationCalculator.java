package nl.duckstudios.pintandpillage.helper;

/**
 * Central place for population capacity calculations.
 */
public class PopulationCalculator {

    public static final int DEFAULT_MAX_LEVEL = 30;

    public int houseCapacity(int level) {
        if (level <= 0) {
            return 0;
        }
        return (int) (8 * Math.pow(level, 2) + 10 * Math.pow(1.32, level));
    }
}
