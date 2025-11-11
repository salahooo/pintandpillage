package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.entity.Coord;
import nl.duckstudios.pintandpillage.entity.Village;
import nl.duckstudios.pintandpillage.entity.buildings.House;
import nl.duckstudios.pintandpillage.entity.buildings.Mine;
import nl.duckstudios.pintandpillage.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DemolitionTest {

    @Test
    void demolish_removesBuilding_and_freesSlot_withoutRefund() {
        Village village = new Village();
        ensureAllResourceTypesPresent(village);

        House house = new House();
        house.setBuildingId(1L);
        house.setPosition(new Coord(2, 2));
        house.setVillage(village);

        village.createBuilding(house);
        int occupiedBeforeDemolish = village.getBuildingCount();
        Map<String, Integer> resourcesBefore = new HashMap<>(village.getVillageResources());

        boolean removed = village.demolishBuilding(house.getBuildingId());

        assertThat(removed).isTrue();
        assertThat(village.hasBuilding(house.getBuildingId())).isFalse();
        assertThat(village.getBuildingCount()).isEqualTo(occupiedBeforeDemolish - 1);
        assertThat(village.getVillageResources()).containsExactlyInAnyOrderEntriesOf(resourcesBefore);

        Mine replacement = new Mine();
        replacement.setBuildingId(2L);
        replacement.setPosition(new Coord(2, 2));
        replacement.setVillage(village);
        replacement.setLastCollected(LocalDateTime.now());
        village.createBuilding(replacement);

        assertThat(village.hasBuilding(replacement.getBuildingId())).isTrue();
        assertThat(village.getBuildingCount()).isEqualTo(occupiedBeforeDemolish);
        assertThat(village.getVillageResources()).containsExactlyInAnyOrderEntriesOf(resourcesBefore);
    }

    @Test
    void demolish_unknownBuildingId_isNoOp() {
        Village village = new Village();
        ensureAllResourceTypesPresent(village);

        House house = new House();
        house.setBuildingId(1L);
        house.setPosition(new Coord(2, 2));
        house.setVillage(village);
        village.createBuilding(house);

        int buildingCountBefore = village.getBuildingCount();
        Map<String, Integer> resourcesBefore = new HashMap<>(village.getVillageResources());

        boolean removed = village.demolishBuilding(999L);

        assertThat(removed).isFalse();
        assertThat(village.hasBuilding(house.getBuildingId())).isTrue();
        assertThat(village.getBuildingCount()).isEqualTo(buildingCountBefore);
        assertThat(village.getVillageResources()).containsExactlyInAnyOrderEntriesOf(resourcesBefore);
    }

    private void ensureAllResourceTypesPresent(Village village) {
        Map<String, Integer> resources = village.getVillageResources();
        for (ResourceType type : ResourceType.values()) {
            resources.putIfAbsent(type.name(), 500);
        }
    }
}
