package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.helper.PopulationCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HousePopulationTest {

    private static final int MAX_LEVEL = PopulationCalculator.DEFAULT_MAX_LEVEL;

    private final PopulationCalculator calculator = new PopulationCalculator();

    @Test
    void capacity_monotonically_increases_with_house_level() {
        assertThat(calculator.houseCapacity(1))
                .as("Level 1 house capacity should be non-negative")
                .isGreaterThanOrEqualTo(0);

        for (int level = 1; level < MAX_LEVEL; level++) {
            int current = calculator.houseCapacity(level);
            int next = calculator.houseCapacity(level + 1);

            assertThat(next)
                    .as("House capacity should increase between level %d and %d", level, level + 1)
                    .isGreaterThan(current);
        }
    }

    @Test
    void boundary_values_and_oracles() {
        int[] levelsToCheck = {1, 2, MAX_LEVEL / 2, MAX_LEVEL - 1, MAX_LEVEL};

        int previous = calculator.houseCapacity(0);
        for (int level : levelsToCheck) {
            int capacity = calculator.houseCapacity(level);

            assertThat(capacity)
                    .as("House capacity must be non-negative at level %d", level)
                    .isGreaterThanOrEqualTo(0);

            assertThat(capacity)
                    .as("House capacity should strictly increase at level %d", level)
                    .isGreaterThan(previous);

            previous = capacity;
        }

        assertThat(calculator.houseCapacity(1)).isEqualTo(21);
        assertThat(calculator.houseCapacity(2)).isEqualTo(49);
        assertThat(calculator.houseCapacity(15)).isEqualTo(2443);
        assertThat(calculator.houseCapacity(30)).isEqualTo(48620);
    }

    @Test
    void zero_and_negative_levels_yield_zero_capacity() {
        assertThat(calculator.houseCapacity(0)).isZero();
        assertThat(calculator.houseCapacity(-3)).isZero();
    }
}
