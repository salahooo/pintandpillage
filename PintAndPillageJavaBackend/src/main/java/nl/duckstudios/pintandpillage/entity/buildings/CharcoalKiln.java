package nl.duckstudios.pintandpillage.entity.buildings;

import lombok.Getter;
import lombok.Setter;
import nl.duckstudios.pintandpillage.model.ResourceType;
import java.util.HashMap;

public class CharcoalKiln extends Building {
    @Getter @Setter
    private String name = "CharcoalKiln";



    public CharcoalKiln() {
        super();
        super.setDescription("Converts wood into charcoal with 60% efficiency");
        super.setProducesResources(false);
    }


    public CharcoalKiln(int level) {
        super();
        super.setLevel(level);
        this.updateBuilding();
    }




    @Override
    public void updateBuilding() {
        int lvl = super.getLevel();
        this.setResourcesRequiredAtGivenLevel(lvl);
        this.setResourcesAtGivenLevel(lvl);
    }

    public void setResourcesAtGivenLevel(int level) {
        long timeSeconds = level * 30 + 40;
        super.setConstructionTimeSeconds(timeSeconds);
    }

    public void setResourcesRequiredAtGivenLevel(int level) {
        super.setResourcesRequiredLevelUp(new HashMap<>() {{
            put(ResourceType.Wood.name(), 45 + level * 5);
            put(ResourceType.Stone.name(), 80 + level * 30);
        }});
    }


    public HashMap<String, Integer> turnWoodIntoCoal(HashMap<String, Integer> resources) {
        HashMap<String, Integer> updated = new HashMap<>(resources);
        int woodAmount = resources.getOrDefault(ResourceType.Wood.name(), 0);
        int producedCharcoal = (int) Math.floor(woodAmount * 0.6);

        updated.put(ResourceType.Wood.name(), 0);
        updated.put(ResourceType.Charcoal.name(), resources.getOrDefault(ResourceType.Charcoal.name(), 0) + producedCharcoal);

        return updated;
    }

    


    

}
