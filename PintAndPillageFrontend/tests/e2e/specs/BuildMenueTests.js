const baseUrl = 'http://localhost:8081';

const ResourceType = {
  Wood: 'Wood',
  Stone: 'Stone',
  Hop: 'Hop',
  Beer: 'Beer'
};

const buildingOptions = [
  createBuildingOption('House', { Wood: 50, Stone: 30 }, 5),
  createBuildingOption('Lumberyard', { Wood: 60, Stone: 40 }, 5, ResourceType.Wood),
  createBuildingOption('Mine', { Wood: 40, Stone: 60 }, 5, ResourceType.Stone),
  createBuildingOption('Farm', { Wood: 45, Stone: 35 }, 5, ResourceType.Hop),
  createBuildingOption('Tavern', { Wood: 55, Stone: 45, Hop: 20 }, 5, ResourceType.Beer, ResourceType.Hop)
];

function createBuildingOption(name, resources, populationRequired, generatesResource = null, requiresResource = null) {
  return {
    name,
    description: `${name} description`,
    resourcesRequiredLevelUp: resources,
    populationRequiredNextLevel: populationRequired,
    constructionTime: '00:05:00',
    buildingLevelRequiredToLevelup: {},
    generatesResource,
    requiresResources: requiresResource
  };
}

const villageListResponse = [
  { villageId: 1, name: 'Test Village' }
];

const createVillageResponse = () => ({
  villageId: 1,
  name: 'Test Village',
  population: 100,
  populationLeft: 100,
  villageResources: {
    Wood: 1000,
    Stone: 1000,
    Hop: 500,
    Beer: 500
  },
  buildings: [
    {
      name: 'Headquarters',
      buildingId: 10,
      level: 1,
      isUnderConstruction: false,
      position: { x: 6, y: 6 }
    }
  ],
  validBuildPositions: [
    { name: 'BuildingTile', position: { x: 1, y: 1 } },
    { name: 'BuildingTile', position: { x: 2, y: 2 } }
  ],
  buildingsThatCanBeBuild: buildingOptions,
  incomingAttacks: [],
  outgoingAttacks: [],
  returningCombatTravels: [],
  quest: { isCompleted: false }
});

const createVillageAfterBuild = () => {
  const village = createVillageResponse();
  village.buildings = village.buildings.concat({
    name: 'House',
    buildingId: 99,
    level: 1,
    isUnderConstruction: false,
    position: { x: 2, y: 2 },
    generatesResource: null
  });
  village.populationLeft = 95;
  village.villageResources = {
    ...village.villageResources,
    Wood: village.villageResources.Wood - 50,
    Stone: village.villageResources.Stone - 30
  };
  return village;
};

describe('Build menu flow', () => {
  beforeEach(() => {
    cy.intercept('GET', `${baseUrl}/api/village/`, (req) => {
      req.reply({ statusCode: 200, body: villageListResponse });
    }).as('getVillageList');

    cy.intercept('GET', `${baseUrl}/api/village/1`, (req) => {
      req.reply({ statusCode: 200, body: createVillageResponse() });
    }).as('getVillage');

    cy.intercept('POST', `${baseUrl}/api/building/build`, (req) => {
      req.reply({ statusCode: 200, body: createVillageAfterBuild() });
    }).as('buildHouse');

    cy.visit(baseUrl, {
      onBeforeLoad(win) {
        win.localStorage.setItem('token', 'test-token');
        win.localStorage.setItem('villageId', '1');
      }
    });

    cy.wait('@getVillageList');
    cy.wait('@getVillage');
  });

  it('opens build menu when clicking a build tile', () => {
    cy.get('[data-testid="build-tile-2-2"]').click({ force: true });
    cy.get('[data-testid="build-menu"]').should('be.visible');
    ['house', 'lumberyard', 'mine', 'farm', 'tavern'].forEach((option) => {
      cy.get(`[data-testid="build-option-${option}"]`).should('exist');
    });
  });

  it('builds a house from the build menu', () => {
    cy.get('[data-testid="build-tile-2-2"]').click({ force: true });
    cy.get('[data-testid="build-option-house"] button').click();
    cy.wait('@buildHouse');
    cy.get('[data-testid="building-house"]').should('exist');
  });
});
