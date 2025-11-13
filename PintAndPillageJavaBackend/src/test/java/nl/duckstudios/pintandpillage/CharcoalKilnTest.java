package nl.duckstudios.pintandpillage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

import nl.duckstudios.pintandpillage.entity.buildings.CharcoalKiln;


/*
    1- get the buidling name "CharcoalKiln"
    2- Set de juiste gebouw kosten bij het aanmaken
    Wood cost level * 5 + 20
    Stone cost level * 30 +80

    3-Set de juiste bouwtijd bij het aanmaken
    bouwtijd = 30 * level + 40

    4- Convert Wood to Charcoal With 40% loss
 */



public class CharcoalKilnTest {

@Test
void whenBuildingCreated_thenBaseCostAndTimeAreCorrect() {
    CharcoalKiln kiln = new CharcoalKiln(0);

    var cost = kiln.getResourcesRequiredLevelUp();

    assertEquals(45, cost.get("Wood"));
    assertEquals(80, cost.get("Stone"));
    assertEquals(40, kiln.getConstructionTimeSeconds());

}

@Test
void whenTurningWoodIntoCoal_then60PercentIsProduced() {
    CharcoalKiln kiln = new CharcoalKiln(0);

    var resources = new java.util.HashMap<String, Integer>();
    resources.put("Wood", 100);
    resources.put("Charcoal", 0);

    var updated = kiln.turnWoodIntoCoal(resources);

    assertEquals(0, updated.get("Wood"));
    assertEquals(60, updated.get("Charcoal"));


}
}