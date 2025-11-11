package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.Exceptions.ProductionConditionsNotMetException;
import nl.duckstudios.pintandpillage.entity.Coord;
import nl.duckstudios.pintandpillage.entity.Village;
import nl.duckstudios.pintandpillage.entity.buildings.Barracks;
import nl.duckstudios.pintandpillage.entity.buildings.DefenceTower;
import nl.duckstudios.pintandpillage.entity.buildings.Farm;
import nl.duckstudios.pintandpillage.entity.buildings.Harbor;
import nl.duckstudios.pintandpillage.entity.buildings.House;
import nl.duckstudios.pintandpillage.entity.buildings.Lumberyard;
import nl.duckstudios.pintandpillage.entity.buildings.Mine;
import nl.duckstudios.pintandpillage.entity.buildings.ResourceBuilding;
import nl.duckstudios.pintandpillage.entity.buildings.Smith;
import nl.duckstudios.pintandpillage.entity.buildings.Storage;
import nl.duckstudios.pintandpillage.entity.buildings.Tavern;
import nl.duckstudios.pintandpillage.entity.buildings.Wall;
import nl.duckstudios.pintandpillage.entity.production.Unit;
import nl.duckstudios.pintandpillage.entity.researching.SpearResearch;
import nl.duckstudios.pintandpillage.helper.BuildingEffectApplier;
import nl.duckstudios.pintandpillage.helper.PlacementRules;
import nl.duckstudios.pintandpillage.helper.PopulationCalculator;
import nl.duckstudios.pintandpillage.helper.UnitFactory;
import nl.duckstudios.pintandpillage.model.BuildPosition;
import nl.duckstudios.pintandpillage.model.ResourceType;
import nl.duckstudios.pintandpillage.model.ResearchType;
import nl.duckstudios.pintandpillage.model.UnitType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildingConstructionTest {

    private final PopulationCalculator populationCalculator = new PopulationCalculator();

    static Stream<Integer> houseLevels() {
        return Stream.of(1, 5, 10);
    }

    @ParameterizedTest
    @MethodSource("houseLevels")
    void house_capacity_matches_population_calculator(int level) {
        House house = new House();
        house.setLevel(level);
        house.updateBuilding();

        assertThat(house.getPopulationCapacity())
                .isEqualTo(populationCalculator.houseCapacity(level));
    }

    static Stream<Arguments> houseMonotonicLevels() {
        return Stream.of(
                Arguments.of(1, 2),
                Arguments.of(2, 3),
                Arguments.of(5, 6)
        );
    }

    @ParameterizedTest
    @MethodSource("houseMonotonicLevels")
    void house_capacity_is_monotonic(int currentLevel, int upgradedLevel) {
        House house = new House();
        house.setLevel(currentLevel);
        house.updateBuilding();
        int baselineCapacity = house.getPopulationCapacity();

        house.setLevel(upgradedLevel);
        house.updateBuilding();

        assertThat(house.getPopulationCapacity()).isGreaterThan(baselineCapacity);
    }

    static Stream<Arguments> resourceBuildingsProduceData() {
        return Stream.of(
                Arguments.of((Supplier<ResourceBuilding>) Lumberyard::new, ResourceType.Wood, null),
                Arguments.of((Supplier<ResourceBuilding>) Mine::new, ResourceType.Stone, null),
                Arguments.of((Supplier<ResourceBuilding>) Farm::new, ResourceType.Hop, null),
                Arguments.of((Supplier<ResourceBuilding>) Tavern::new, ResourceType.Beer, ResourceType.Hop)
        );
    }

    @ParameterizedTest
    @MethodSource("resourceBuildingsProduceData")
    void resource_buildings_generate_expected_outputs(Supplier<ResourceBuilding> supplier,
                                                      ResourceType expectedResource,
                                                      ResourceType expectedRequirement) {
        ResourceBuilding building = supplier.get();
        building.setLevel(1);
        building.updateBuilding();

        assertThat(building.getGeneratesResource())
                .isEqualTo(expectedResource);
        assertThat(building.getResourcesPerHour())
                .isGreaterThan(0);
        if (expectedRequirement != null) {
            assertThat(building.getRequiresResources())
                    .isEqualTo(expectedRequirement);
        }
    }

    static Stream<Supplier<ResourceBuilding>> resourceBuildingUpgradeData() {
        return Stream.of(
                (Supplier<ResourceBuilding>) Lumberyard::new,
                (Supplier<ResourceBuilding>) Mine::new,
                (Supplier<ResourceBuilding>) Farm::new,
                (Supplier<ResourceBuilding>) Tavern::new
        );
    }

    @ParameterizedTest
    @MethodSource("resourceBuildingUpgradeData")
    void upgrading_resource_buildings_increases_production(Supplier<ResourceBuilding> supplier) {
        ResourceBuilding building = supplier.get();
        building.setLevel(1);
        building.updateBuilding();
        int baseProduction = building.getResourcesPerHour();

        building.setLevel(2);
        building.updateBuilding();

        assertThat(building.getResourcesPerHour())
                .isGreaterThan(baseProduction);
    }

    @Test
    void storage_increases_village_resource_limit() {
        Village village = new Village();
        int initialLimit = village.getResourceLimit();

        BuildingEffectApplier.registerBuiltBuilding(village, new Storage(), 1);

        assertThat(village.getResourceLimit()).isGreaterThan(initialLimit);
    }

    @Test
    void barracks_consumes_beer_when_queueing_units() {
        Village village = new Village();
        BuildingEffectApplier.registerBuiltBuilding(village, new House(), 5);
        Barracks barracks = new Barracks();
        BuildingEffectApplier.registerBuiltBuilding(village, barracks, 1);

        SpearResearch research = new SpearResearch();
        research.setResearchLevel(1);
        research.setVillage(village);
        village.getCompletedResearches().add(research);

        Unit spear = UnitFactory.getUnitStatic(UnitType.Spear.name());
        int beerBefore = village.getVillageResources().get(ResourceType.Beer.name());

        barracks.produceUnit(spear, 1);

        assertThat(barracks.getProductionQueue()).isNotEmpty();
        assertThat(village.getVillageResources().get(ResourceType.Beer.name()))
                .isLessThan(beerBefore);
    }

    @Test
    void barracks_requires_population_before_production() {
        Village village = new Village();
        Barracks barracks = new Barracks();
        BuildingEffectApplier.registerBuiltBuilding(village, barracks, 1);

        SpearResearch research = new SpearResearch();
        research.setResearchLevel(1);
        research.setVillage(village);
        village.getCompletedResearches().add(research);

        Unit spear = UnitFactory.getUnitStatic(UnitType.Spear.name());

        assertThatThrownBy(() -> barracks.produceUnit(spear, 1))
                .isInstanceOf(ProductionConditionsNotMetException.class)
                .hasMessageContaining("population");
    }

    static Stream<Arguments> wallPlacementCases() {
        Village village = new Village();
        BuildPosition[] positions = village.getValidBuildPositions();
        Coord allowed = Arrays.stream(positions)
                .filter(bp -> bp.allowedBuilding != null && bp.allowedBuilding.equals(Wall.class.getSimpleName()))
                .findFirst()
                .map(bp -> bp.position)
                .orElseThrow();
        Coord adjacent = new Coord(allowed.getX() + 1, allowed.getY());
        return Stream.of(
                Arguments.of(allowed, true),
                Arguments.of(adjacent, false)
        );
    }

    @ParameterizedTest
    @MethodSource("wallPlacementCases")
    void wall_can_only_be_placed_on_dedicated_tile(Coord target, boolean expected) {
        Village village = new Village();
        boolean allowed = PlacementRules.isPlacementAllowed(
                village.getValidBuildPositions(),
                Wall.class.getSimpleName(),
                target
        );

        assertThat(allowed).isEqualTo(expected);
    }

    static Stream<Arguments> harborPlacementCases() {
        Village village = new Village();
        BuildPosition[] positions = village.getValidBuildPositions();
        Coord harborTile = Arrays.stream(positions)
                .filter(bp -> "Harbor".equals(bp.allowedBuilding))
                .findFirst()
                .map(bp -> bp.position)
                .orElseThrow();
        Coord landTile = Arrays.stream(positions)
                .filter(bp -> bp.allowedBuilding == null)
                .findFirst()
                .map(bp -> bp.position)
                .orElseGet(() -> new Coord(harborTile.getX() + 1, harborTile.getY()));
        Coord offset = new Coord(harborTile.getX(), harborTile.getY() - 1);
        return Stream.of(
                Arguments.of(harborTile, true),
                Arguments.of(landTile, false),
                Arguments.of(offset, false)
        );
    }

    @ParameterizedTest
    @MethodSource("harborPlacementCases")
    void harbor_requires_coastal_tile(Coord target, boolean expected) {
        Village village = new Village();
        boolean allowed = PlacementRules.isPlacementAllowed(
                village.getValidBuildPositions(),
                Harbor.class.getSimpleName(),
                target
        );

        assertThat(allowed).isEqualTo(expected);
    }

    @Test
    void harbor_unlocks_transport_ships() {
        Harbor harbor = new Harbor();
        harbor.setLevel(1);
        harbor.updateBuilding();

        assertThat(harbor.getUnlockedUnitsData())
                .anyMatch(data -> data.unit.getUnitName() == UnitType.TransportShip);
    }

    @Test
    void smith_completes_research_when_time_has_elapsed() {
        Village village = new Village();
        BuildingEffectApplier.registerBuiltBuilding(village, new House(), 5);
        Smith smith = new Smith();
        BuildingEffectApplier.registerBuiltBuilding(village, smith, 1);

        SpearResearch research = new SpearResearch();
        smith.startResearch(research);
        smith.setCurrentResearchFinishTime(LocalDateTime.now().minusSeconds(1));

        village.updateVillageState();

        assertThat(village.getCompletedResearches())
                .anyMatch(done -> done.getResearchName() == ResearchType.Spear);
    }

    static Stream<Arguments> defenceTowerLevelPairs() {
        return Stream.of(
                Arguments.of(1, 2),
                Arguments.of(2, 3)
        );
    }

    @ParameterizedTest
    @MethodSource("defenceTowerLevelPairs")
    void defence_tower_bonus_increases_with_level(int currentLevel, int upgradedLevel) {
        DefenceTower tower = new DefenceTower();
        tower.setLevel(currentLevel);
        tower.updateBuilding();
        int baseBonus = tower.getDefenceBonus();

        tower.setLevel(upgradedLevel);
        tower.updateBuilding();

        assertThat(tower.getDefenceBonus()).isGreaterThan(baseBonus);
    }
}
