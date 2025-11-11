const BASE_URL = 'http://localhost:8081';
const API_BASE = `${BASE_URL}/api`;
const VILLAGE_ID = 1;

const ResourceType = {
  Wood: 'Wood',
  Stone: 'Stone',
  Hop: 'Hop',
  Beer: 'Beer',
};

const createBuildOption = (name, resources, populationRequired, generatesResource = null, requiresResource = null) => ({
  name,
  description: `${name} description`,
  resourcesRequiredLevelUp: resources,
  populationRequiredNextLevel: populationRequired,
  constructionTime: '00:05:00',
  buildingLevelRequiredToLevelup: {},
  generatesResource,
  requiresResources: requiresResource,
});

const buildMenuOptions = [
  createBuildOption('House', { Wood: 80, Stone: 50 }, 5),
  createBuildOption('Lumberyard', { Wood: 60, Stone: 40 }, 6, ResourceType.Wood),
  createBuildOption('Mine', { Wood: 40, Stone: 70 }, 6, ResourceType.Stone),
  createBuildOption('Farm', { Wood: 50, Stone: 30 }, 8, ResourceType.Hop),
  createBuildOption('Tavern', { Wood: 55, Stone: 45, Hop: 20 }, 8, ResourceType.Beer, ResourceType.Hop),
  createBuildOption('Barracks', { Wood: 120, Stone: 80, Beer: 30 }, 10),
];

let buildingIdCounter = 2000;
const nextBuildingId = () => buildingIdCounter++;

const createBuilding = (name, overrides = {}) => {
  const building = {
    buildingId: overrides.buildingId || nextBuildingId(),
    name,
    level: overrides.level ?? 1,
    position: overrides.position || { x: 2, y: 2 },
    isUnderConstruction: overrides.isUnderConstruction ?? false,
    generatesResource: overrides.generatesResource ?? null,
    unlockedUnitsData: [],
    unitsUnlockedAtLevel: [],
    ...overrides,
  };
  building.unlockedUnitsData = building.unlockedUnitsData || [];
  building.unitsUnlockedAtLevel = building.unitsUnlockedAtLevel || [];
  return building;
};

const deepCopy = (value) => JSON.parse(JSON.stringify(value));

const baseVillageTemplate = {
  villageId: VILLAGE_ID,
  name: 'QA Village',
  population: 100,
  populationLeft: 60,
  villageResources: {
    Wood: 500,
    Stone: 400,
    Hop: 40,
    Beer: 200,
  },
  buildings: [
    createBuilding('Headquarters', { buildingId: 1000, level: 4, position: { x: 6, y: 6 }, allowedBuilding: 'Headquarters' }),
    createBuilding('Storage', { buildingId: 1001, level: 3, position: { x: 7, y: 6 }, allowedBuilding: 'Storage' }),
  ],
  validBuildPositions: [
    { name: 'BuildingTile', position: { x: 2, y: 2 } },
    { name: 'BuildingTile', position: { x: 3, y: 3 } },
    { name: 'BuildingTile', position: { x: 4, y: 4 } },
  ],
  buildingsThatCanBeBuild: buildMenuOptions,
  unitsInVillage: [
    { unit: { unitName: 'Spear' }, amount: 10 },
  ],
  incomingAttacks: [],
  outgoingAttacks: [],
  returningCombatTravels: [],
  quest: { isCompleted: false },
};

const createVillageState = (overrides = {}) => ({
  ...deepCopy(baseVillageTemplate),
  ...overrides,
  villageResources: {
    ...deepCopy(baseVillageTemplate.villageResources),
    ...(overrides.villageResources || {}),
  },
  buildings: overrides.buildings ? overrides.buildings : deepCopy(baseVillageTemplate.buildings),
  validBuildPositions: overrides.validBuildPositions ? overrides.validBuildPositions : deepCopy(baseVillageTemplate.validBuildPositions),
  buildingsThatCanBeBuild: deepCopy(baseVillageTemplate.buildingsThatCanBeBuild),
  unitsInVillage: overrides.unitsInVillage ? overrides.unitsInVillage : deepCopy(baseVillageTemplate.unitsInVillage),
});

const stubVillageLifecycle = (initialState) => {
  const stateRef = { current: deepCopy(initialState) };

  cy.intercept('GET', `${API_BASE}/village/`, {
    statusCode: 200,
    body: [{ villageId: VILLAGE_ID, name: initialState.name }],
  }).as('getVillageList');

  cy.intercept('GET', `${API_BASE}/village/${VILLAGE_ID}`, (req) => {
    req.reply({ statusCode: 200, body: stateRef.current });
  }).as('getVillage');

  return stateRef;
};

const registerBuildIntercept = (stateRef, updatedState, alias, expectedBuildingType) => {
  cy.intercept('POST', `${API_BASE}/building/build`, (req) => {
    if (expectedBuildingType) {
      expect(req.body.buildingType).to.eq(expectedBuildingType);
    }
    stateRef.current = deepCopy(updatedState);
    req.reply({ statusCode: 200, body: stateRef.current });
  }).as(alias);
};

const registerTrainIntercept = (stateRef, updatedState, alias, expectedUnitType) => {
  cy.intercept('POST', `${API_BASE}/production/train`, (req) => {
    if (expectedUnitType) {
      expect(req.body.unitType).to.eq(expectedUnitType);
    }
    stateRef.current = deepCopy(updatedState);
    req.reply({ statusCode: 200, body: stateRef.current });
  }).as(alias);
};

const visitVillageScreen = () => {
  const hasCustomLogin = Boolean(Cypress.Commands && Cypress.Commands._commands && Cypress.Commands._commands.login);
  if (hasCustomLogin) {
    cy.login();
  }
  cy.visit(BASE_URL, {
    onBeforeLoad(win) {
      if (!hasCustomLogin) {
        win.localStorage.setItem('token', 'qa-token');
        win.localStorage.setItem('villageId', `${VILLAGE_ID}`);
      }
    },
  });
  cy.wait('@getVillageList');
  cy.wait('@getVillage');
  cy.get('[data-testid^="build-tile-"]', { timeout: 10000 }).should('have.length.at.least', 1);
};

const openBuildMenuForTile = (x, y) => {
  cy.get(`[data-testid="build-tile-${x}-${y}"]`).scrollIntoView().click({ force: true });
  cy.get('body').then(($body) => {
    if ($body.find('[data-testid="build-menu"]').length) {
      cy.get('[data-testid="build-menu"]').should('be.visible');
    } else {
      cy.get('.build-menu').should('be.visible');
    }
  });
};

const clickBuildAction = (selectorId) => {
  cy.get('body').then(($body) => {
    const dataCySelector = `[data-cy="${selectorId}"]`;
    const dataTestIdSelector = `[data-testid="build-option-${selectorId.replace('build-', '')}"] button`;
    if ($body.find(dataCySelector).length) {
      cy.get(dataCySelector).click();
    } else {
      cy.get(dataTestIdSelector).first().click();
    }
  });
};

const assertResourceValue = (displayId, expectedValue) => {
  cy.get(displayId, { timeout: 10000 }).should('contain', expectedValue);
};

const assertArmyCount = (expectedValue) => {
  cy.get('#army-count', { timeout: 10000 }).should('contain', expectedValue);
};

const setTrainingAmount = (unitName, amount) => {
  cy.contains('.unitDescription h1', unitName, { timeout: 10000 })
    .closest('.unitContainer')
    .find('input[type="range"]')
    .invoke('val', amount)
    .trigger('input');
  cy.contains('.unitDescription h1', unitName)
    .closest('.unitContainer')
    .find('input[type="number"]')
    .clear()
    .type(String(amount));
};

const spearUnitBlueprint = {
  key: 'spear',
  unit: {
    unitName: 'Spear',
    description: 'Baseline melee unit',
    attack: 10,
    defence: 5,
    health: 50,
    speed: 10,
    resourcesRequiredToProduce: { Wood: 20, Stone: 10, Beer: 5 },
    populationRequiredPerUnit: 1,
    baseTimeToProduce: 60,
    researchRequired: 'None',
  },
  levelUnlocked: 1,
};

const spearUnlockRequirement = [{ unitType: 'Spear', level: 1 }];

describe('BuildingsAndTroopTests', () => {
  beforeEach(() => {
    buildingIdCounter = 3000;
  });

  it('builds a house so the player gains more population', () => {
    const initialVillage = createVillageState({
      population: 80,
      populationLeft: 50,
      villageResources: { Wood: 500, Stone: 420 },
    });
    const house = createBuilding('House', { position: { x: 2, y: 2 } });
    const afterHouse = createVillageState({
      population: 95,
      populationLeft: 95,
      villageResources: { Wood: 450, Stone: 390 },
      buildings: [...initialVillage.buildings, house],
    });
    const stateRef = stubVillageLifecycle(initialVillage);
    registerBuildIntercept(stateRef, afterHouse, 'buildHouse', 'House');

    visitVillageScreen();
    openBuildMenuForTile(2, 2);
    clickBuildAction('build-house');
    cy.wait('@buildHouse');
    assertResourceValue('#population', '95');
  });

  it('builds a lumberyard to increase wood resources', () => {
    const initialVillage = createVillageState({
      villageResources: { Wood: 520, Stone: 380 },
    });
    const lumberyard = createBuilding('Lumberyard', { position: { x: 3, y: 3 }, generatesResource: ResourceType.Wood });
    const afterLumberyard = createVillageState({
      villageResources: { Wood: 620, Stone: 360 },
      buildings: [...initialVillage.buildings, lumberyard],
    });
    const stateRef = stubVillageLifecycle(initialVillage);
    registerBuildIntercept(stateRef, afterLumberyard, 'buildLumberyard', 'Lumberyard');

    visitVillageScreen();
    openBuildMenuForTile(2, 2);
    clickBuildAction('build-lumberyard');
    cy.wait('@buildLumberyard');
    assertResourceValue('#wood', '620');
  });

  it('builds a mine to gain more stone resources', () => {
    const initialVillage = createVillageState({
      villageResources: { Wood: 540, Stone: 360 },
    });
    const mine = createBuilding('Mine', { position: { x: 4, y: 4 }, generatesResource: ResourceType.Stone });
    const afterMine = createVillageState({
      villageResources: { Wood: 510, Stone: 460 },
      buildings: [...initialVillage.buildings, mine],
    });
    const stateRef = stubVillageLifecycle(initialVillage);
    registerBuildIntercept(stateRef, afterMine, 'buildMine', 'Mine');

    visitVillageScreen();
    openBuildMenuForTile(2, 2);
    clickBuildAction('build-mine');
    cy.wait('@buildMine');
    assertResourceValue('#stone', '460');
  });

  it('builds a farm to unlock hop production', () => {
    const initialVillage = createVillageState({
      villageResources: { Hop: 0 },
    });
    const farm = createBuilding('Farm', { position: { x: 3, y: 3 }, generatesResource: ResourceType.Hop });
    const afterFarm = createVillageState({
      villageResources: { Hop: 120 },
      buildings: [...initialVillage.buildings, farm],
    });
    const stateRef = stubVillageLifecycle(initialVillage);
    registerBuildIntercept(stateRef, afterFarm, 'buildFarm', 'Farm');

    visitVillageScreen();
    openBuildMenuForTile(2, 2);
    clickBuildAction('build-farm');
    cy.wait('@buildFarm');
    assertResourceValue('#hop', '120');
  });

  it('builds barracks so population and beer can be converted into warriors later', () => {
    const initialVillage = createVillageState({
      population: 80,
      populationLeft: 55,
      villageResources: { Beer: 180, Wood: 600, Stone: 500 },
    });
    const barracks = createBuilding('Barracks', { position: { x: 2, y: 2 } });
    const afterBarracks = createVillageState({
      population: 70,
      populationLeft: 70,
      villageResources: { Beer: 150, Wood: 480, Stone: 420 },
      buildings: [...initialVillage.buildings, barracks],
    });
    const stateRef = stubVillageLifecycle(initialVillage);
    registerBuildIntercept(stateRef, afterBarracks, 'buildBarracks', 'Barracks');

    visitVillageScreen();
    openBuildMenuForTile(2, 2);
    clickBuildAction('build-barracks');
    cy.wait('@buildBarracks');
    assertResourceValue('#population', '70');
    assertResourceValue('#beer', '150');
  });

});

describe('BuildingsAndTroopTests', () => {
  beforeEach(() => {
    buildingIdCounter = 4000;
  });

  it('trains viking warriors inside the barracks', () => {
    const barracks = createBuilding('Barracks', {
      position: { x: 2, y: 2 },
      level: 5,
      unlockedUnitsData: [spearUnitBlueprint],
      unitsUnlockedAtLevel: spearUnlockRequirement,
    });
    const initialVillage = createVillageState({
      villageResources: { Beer: 160 },
      buildings: [...deepCopy(baseVillageTemplate.buildings), deepCopy(barracks)],
      unitsInVillage: [{ unit: { unitName: 'Spear' }, amount: 10 }],
    });
    const afterTraining = createVillageState({
      villageResources: { Beer: 140 },
      buildings: [...deepCopy(baseVillageTemplate.buildings), deepCopy(barracks)],
      unitsInVillage: [{ unit: { unitName: 'Spear' }, amount: 15 }],
    });
    const stateRef = stubVillageLifecycle(initialVillage);
    registerTrainIntercept(stateRef, afterTraining, 'trainSpear', 'Spear');

    visitVillageScreen();
    cy.get('[data-testid="building-barracks"]').scrollIntoView().click({ force: true });
    cy.get('.barackContainer', { timeout: 10000 }).should('be.visible');
    setTrainingAmount('Spear', 1);
    cy.get('body').then(($body) => {
      if ($body.find('[data-cy="train-spear"]').length) {
        cy.get('[data-cy="train-spear"]').click();
      } else if ($body.find('[data-testid="train-spear"]').length) {
        cy.get('[data-testid="train-spear"]').click();
      } else {
        cy.contains('.unitDescription h1', 'Spear')
          .closest('.unitContainer')
          .find('button')
          .contains('Train')
          .click();
      }
    });
    cy.wait('@trainSpear');
    assertArmyCount('15');
  });
});
