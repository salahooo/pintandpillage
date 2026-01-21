package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.Exceptions.ProductionConditionsNotMetException;
import nl.duckstudios.pintandpillage.entity.Village;
import nl.duckstudios.pintandpillage.entity.buildings.ProductionBuilding;
import nl.duckstudios.pintandpillage.entity.production.Unit;
import nl.duckstudios.pintandpillage.helper.ResourceManager;
import nl.duckstudios.pintandpillage.helper.UnitFactory;
import nl.duckstudios.pintandpillage.model.UnitType;
import nl.duckstudios.pintandpillage.model.Unlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitCreationTest {

    private static class TestProductionBuilding extends ProductionBuilding {
        void setResourceManager(ResourceManager manager) {
            this.resourceManager = manager;
        }

        @Override
        public void updateBuilding() {
            // No-op; test controls production rules directly.
        }
    }

    @Mock
    private ResourceManager resourceManager;

    @Mock
    private Village village;

    @InjectMocks
    private TestProductionBuilding productionBuilding;

    private Unit transportShip;

    @BeforeEach
    void setUp() {
        transportShip = UnitFactory.getUnitStatic(UnitType.TransportShip.name());
        // Align the base unit field with the subclass getter for production checks.
        transportShip.populationRequiredPerUnit = transportShip.getPopulationRequiredPerUnit();
        productionBuilding.setVillage(village);
        productionBuilding.setLevel(1);
        productionBuilding.setUnitsUnlockedAtLevel(List.of(new Unlock(UnitType.TransportShip, 1)));
        productionBuilding.setResourceManager(resourceManager);
    }

    @Test
    void shouldCreateNewUnit_whenValidInput() {
        // Arrange: enough resources and population to train the unit.
        int amount = 1;
        Map<String, Integer> costs = transportShip.getResourcesRequiredToProduce();

        when(resourceManager.hasEnoughResourcesAvailable(village, costs, amount)).thenReturn(true);
        when(village.hasEnoughPopulation(transportShip.getPopulationRequiredPerUnit(), amount)).thenReturn(true);

        // Act: queue the unit for production.
        productionBuilding.produceUnit(transportShip, amount);

        // Assert: resources are deducted and the queue contains the unit.
        assertThat(productionBuilding.getProductionQueue()).hasSize(1);
        verify(resourceManager).subtractResources(village, costs);
    }

    @Test
    void shouldNotCreateUnit_whenNotEnoughResources() {
        // Arrange: resource manager blocks the request.
        int amount = 1;
        Map<String, Integer> costs = transportShip.getResourcesRequiredToProduce();

        when(resourceManager.hasEnoughResourcesAvailable(village, costs, amount)).thenReturn(false);
        when(village.hasEnoughPopulation(transportShip.getPopulationRequiredPerUnit(), amount)).thenReturn(true);

        // Act + Assert: production fails and no resources are deducted.
        assertThatThrownBy(() -> productionBuilding.produceUnit(transportShip, amount))
                .isInstanceOf(ProductionConditionsNotMetException.class)
                .hasMessageContaining("Not enough resources");
        verify(resourceManager, never()).subtractResources(eq(village), eq(costs));
    }

    @Test
    void shouldNotCreateUnit_whenPopulationLimitReached() {
        // Arrange: insufficient population prevents training.
        int amount = 2;
        Map<String, Integer> costs = transportShip.getResourcesRequiredToProduce();

        when(village.hasEnoughPopulation(transportShip.getPopulationRequiredPerUnit(), amount)).thenReturn(false);

        // Act + Assert: production fails with a population error.
        assertThatThrownBy(() -> productionBuilding.produceUnit(transportShip, amount))
                .isInstanceOf(ProductionConditionsNotMetException.class)
                .hasMessageContaining("population");
        verify(resourceManager, never()).subtractResources(eq(village), eq(costs));
    }

    @Test
    void shouldIncreasePopulationAfterTraining() {
        // Arrange: queue a unit and fast-forward production time.
        int amount = 1;
        Map<String, Integer> costs = transportShip.getResourcesRequiredToProduce();

        when(resourceManager.hasEnoughResourcesAvailable(village, costs, amount)).thenReturn(true);
        when(village.hasEnoughPopulation(transportShip.getPopulationRequiredPerUnit(), amount)).thenReturn(true);

        productionBuilding.produceUnit(transportShip, amount);
        productionBuilding.setLastCollected(LocalDateTime.now()
                .minusSeconds(transportShip.getBaseSecondsToProduce() + 1));

        // Act: process the production queue so the unit is added to the village.
        productionBuilding.checkProduction();

        // Assert: training adds the unit to the village roster.
        verify(village).addUnit(eq(transportShip), eq(amount));
        assertThat(productionBuilding.getProductionQueue()).isEmpty();
    }
}
