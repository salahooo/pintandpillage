package nl.duckstudios.pintandpillage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import nl.duckstudios.pintandpillage.entity.buildings.CharcoalKiln;


public class TDDCharcoalKilnTest {

@Test
void whenBuildingCreated_thenBaseCostAndTimeAreCorrect() {
    CharcoalKiln kiln = new CharcoalKiln(0);

    // Haal de map op
    var cost = kiln.getResourcesRequiredLevelUp();

    assertEquals(45, cost.get("Wood"), "Houtkosten moeten 45 zijn bij level 0");
    assertEquals(80, cost.get("Stone"), "Steenkosten moeten 80 zijn bij level 0");
    assertEquals(40, kiln.getConstructionTimeSeconds(), "Bouwtijd moet 40 seconden zijn bij level 0");
}

@Test
void whenTurningWoodIntoCoal_then60PercentIsProduced() {
    CharcoalKiln kiln = new CharcoalKiln(1);

    // Arrange – begin met een resource map met 100 hout
    var resources = new java.util.HashMap<String, Integer>();
    resources.put("Wood", 100);
    resources.put("Charcoal", 0);

    // Act – voer de conversie uit
    var updated = kiln.turnWoodIntoCoal(resources);

    // Assert – controleer dat hout verbruikt is en houtskool is geproduceerd
    assertEquals(0, updated.get("Wood"), "Na conversie mag geen hout overblijven");
    assertEquals(60, updated.get("Charcoal"), "100 hout moet 60 houtskool opleveren");
}

}