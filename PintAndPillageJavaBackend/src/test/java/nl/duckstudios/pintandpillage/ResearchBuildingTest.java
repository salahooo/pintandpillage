package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.Exceptions.ResearchConditionsNotMetException;
import nl.duckstudios.pintandpillage.entity.Village;
import nl.duckstudios.pintandpillage.entity.buildings.ResearchBuilding;
import nl.duckstudios.pintandpillage.entity.researching.Research;
import nl.duckstudios.pintandpillage.helper.ResourceManager;
import nl.duckstudios.pintandpillage.model.ResearchType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ResearchBuildingTest {

    private static class TestResearchBuilding extends ResearchBuilding {
        void setResourceManager(ResourceManager manager) {
            this.resourceManager = manager;
        }

        @Override
        public void updateBuilding() {
            // No-op for unit tests.
        }
    }

    @Test
    void shouldStartResearchWhenConditionsMet() {
        // Arrange: mock dependencies for resources and research payload.
        TestResearchBuilding building = new TestResearchBuilding();
        ResourceManager resourceManager = mock(ResourceManager.class);
        Village village = mock(Village.class);
        Research research = mock(Research.class);
        Map<String, Integer> costs = Map.of("Wood", 100, "Stone", 50);

        building.setResourceManager(resourceManager);
        building.setVillage(village);
        building.setLevel(3);

        when(research.getResourcesRequiredToResearch()).thenReturn(costs);
        when(research.getBaseSecondsToResearch()).thenReturn(10L);
        when(research.getBuildingLevelRequirement()).thenReturn(1);
        when(resourceManager.hasEnoughResourcesAvailable(village, costs)).thenReturn(true);

        // Act: start a research.
        building.startResearch(research);

        // Assert: state is updated and resources are deducted.
        assertThat(building.isResearchInProgress()).isTrue();
        assertThat(building.getCurrentResearch()).isEqualTo(research);
        verify(research).setVillage(village);
        verify(resourceManager).subtractResources(village, costs);
    }

    @Test
    void shouldCompleteResearchWhenFinishTimeElapsed() {
        // Arrange: start research and then move finish time to the past.
        TestResearchBuilding building = new TestResearchBuilding();
        ResourceManager resourceManager = mock(ResourceManager.class);
        Village village = mock(Village.class);
        Research research = mock(Research.class);
        Map<String, Integer> costs = Map.of("Wood", 100);

        building.setResourceManager(resourceManager);
        building.setVillage(village);
        building.setLevel(3);

        when(research.getResourcesRequiredToResearch()).thenReturn(costs);
        when(research.getBaseSecondsToResearch()).thenReturn(1L);
        when(research.getBuildingLevelRequirement()).thenReturn(1);
        when(research.getResearchLevel()).thenReturn(0);
        when(research.getResearchName()).thenReturn(ResearchType.Spear);
        when(resourceManager.hasEnoughResourcesAvailable(village, costs)).thenReturn(true);

        building.startResearch(research);
        building.setCurrentResearchFinishTime(LocalDateTime.now().minusSeconds(1));

        // Act: tick the building update to process completion.
        building.updateBuildingState();

        // Assert: research is completed and added to the village.
        assertThat(building.isResearchInProgress()).isFalse();
        assertThat(building.getCurrentResearch()).isNull();
        verify(village).addCompleteResearch(research);
        verify(research).setResearchLevel(1);
    }

    @Test
    void shouldThrowWhenResourcesMissing() {
        // Arrange: deny resources to trigger the validation error.
        TestResearchBuilding building = new TestResearchBuilding();
        ResourceManager resourceManager = mock(ResourceManager.class);
        Village village = mock(Village.class);
        Research research = mock(Research.class);
        Map<String, Integer> costs = Map.of("Wood", 100);

        building.setResourceManager(resourceManager);
        building.setVillage(village);
        building.setLevel(3);

        when(research.getResourcesRequiredToResearch()).thenReturn(costs);
        when(research.getBuildingLevelRequirement()).thenReturn(1);
        when(resourceManager.hasEnoughResourcesAvailable(village, costs)).thenReturn(false);

        // Act + Assert: insufficient resources stops the research.
        assertThatThrownBy(() -> building.startResearch(research))
                .isInstanceOf(ResearchConditionsNotMetException.class)
                .hasMessageContaining("Not enough resources");
        verify(resourceManager, never()).subtractResources(any(), any());
    }
}
