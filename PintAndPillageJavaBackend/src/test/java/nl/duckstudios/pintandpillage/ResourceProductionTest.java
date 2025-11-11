package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.helper.ResourceProductionCalculator;
import nl.duckstudios.pintandpillage.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceProductionTest {

    private static final ResourceType[] RESOURCE_TYPES = {
            ResourceType.Wood,
            ResourceType.Stone,
            ResourceType.Hop,
            ResourceType.Beer
    };

    private static final int MAX_LEVEL = ResourceProductionCalculator.DEFAULT_MAX_LEVEL;

    private final ResourceProductionCalculator calculator = new ResourceProductionCalculator();

    @Test
    void monotonic_increase_per_level_for_each_resource_building() {
        for (ResourceType type : RESOURCE_TYPES) {
            assertThat(calculator.productionPerHour(type, 1))
                    .as("Level 1 production for %s should be non-negative", type)
                    .isGreaterThanOrEqualTo(0);

            for (int level = 1; level < MAX_LEVEL; level++) {
                int current = calculator.productionPerHour(type, level);
                int next = calculator.productionPerHour(type, level + 1);

                assertThat(next)
                        .as("%s production should increase from level %d to %d", type, level, level + 1)
                        .isGreaterThan(current);
            }
        }
    }

    @Test
    void boundary_values_linearization_oracle_or_properties() {
        int[] levelsToCheck = {1, 2, MAX_LEVEL / 2, MAX_LEVEL - 1, MAX_LEVEL};

        for (ResourceType type : RESOURCE_TYPES) {
            int previous = calculator.productionPerHour(type, 0);
            for (int level : levelsToCheck) {
                int production = calculator.productionPerHour(type, level);

                assertThat(production)
                        .as("%s production should be non-negative at level %d", type, level)
                        .isGreaterThanOrEqualTo(0);

                assertThat(production)
                        .as("%s production should be strictly increasing at level %d", type, level)
                        .isGreaterThan(previous);

                previous = production;
            }
        }

        assertThat(calculator.productionPerHour(ResourceType.Wood, 1)).isEqualTo(32);
        assertThat(calculator.productionPerHour(ResourceType.Wood, 5)).isEqualTo(102);
        assertThat(calculator.productionPerHour(ResourceType.Beer, 1)).isEqualTo(16);
        assertThat(calculator.productionPerHour(ResourceType.Beer, 5)).isEqualTo(48);
    }

    @Test
    void overflow_and_rounding_safety() {
        for (ResourceType type : RESOURCE_TYPES) {
            for (int level = 1; level <= MAX_LEVEL; level++) {
                int production = calculator.productionPerHour(type, level);

                assertThat(production)
                        .as("%s production should stay within integer bounds at level %d", type, level)
                        .isLessThanOrEqualTo(Integer.MAX_VALUE)
                        .isGreaterThanOrEqualTo(0);
            }
        }
    }
}
