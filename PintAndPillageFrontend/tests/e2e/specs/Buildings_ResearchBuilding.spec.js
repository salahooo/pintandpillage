const BASE_URL = 'http://localhost:8081';
const API_BASE = `${BASE_URL}/api`;
const VILLAGE_ID = 1;
const SMITH_BUILDING_ID = 101;
const BARRACKS_BUILDING_ID = 102;
const RESEARCH_IN_PROGRESS = 'Spear';
const UNLOCKED_RESEARCH = 'Axe';

const researchSpear = {
  researchName: RESEARCH_IN_PROGRESS,
  researchLevel: 0,
  baseSecondsToResearch: 10,
  secondsToResearch: '00:00:10',
  buildingLevelRequirement: 1,
  resourcesRequiredToResearch: { Wood: 100, Stone: 50 },
};

const researchAxe = {
  researchName: UNLOCKED_RESEARCH,
  researchLevel: 1,
  baseSecondsToResearch: 7200,
  secondsToResearch: '02:00:00',
  buildingLevelRequirement: 3,
  resourcesRequiredToResearch: { Wood: 150, Stone: 80 },
};

const spearUnit = {
  unitName: 'Spear',
  description: 'Basic unit with spear',
  attack: 15,
  defence: 15,
  health: 20,
  speed: 10,
  populationRequiredPerUnit: 1,
  baseTimeToProduce: '00:00:10',
  resourcesRequiredToProduce: { Wood: 15, Beer: 15 },
  researchRequired: 'Spear',
};

const axeUnit = {
  unitName: 'Axe',
  description: 'Axe unit unlocked by research',
  attack: 15,
  defence: 15,
  health: 20,
  speed: 10,
  populationRequiredPerUnit: 1,
  baseTimeToProduce: '00:00:10',
  resourcesRequiredToProduce: { Wood: 15, Beer: 15 },
  researchRequired: 'Axe',
};

const smithBuilding = {
  buildingId: SMITH_BUILDING_ID,
  name: 'Smith',
  level: 3,
  position: { x: 5, y: 5 },
  isUnderConstruction: false,
  isResearchInProgress: false,
  currentResearch: null,
  researchTimeLeft: '00:00:00',
  resourcesRequiredLevelUp: { Wood: 1000, Stone: 150 },
  populationRequiredNextLevel: 2,
};

const barracksBuilding = {
  buildingId: BARRACKS_BUILDING_ID,
  name: 'Barracks',
  level: 4,
  position: { x: 6, y: 5 },
  isUnderConstruction: false,
  productionQueue: [],
  unitsUnlockedAtLevel: [
    { unitType: 'Spear', level: 1 },
    { unitType: 'Axe', level: 4 },
  ],
  unlockedUnitsData: [
    { unit: spearUnit, levelUnlocked: 1 },
    { unit: axeUnit, levelUnlocked: 4 },
  ],
  resourcesRequiredLevelUp: { Wood: 1000, Stone: 800 },
  populationRequiredNextLevel: 2,
};

const createVillageState = (overrides = {}) => ({
  villageId: VILLAGE_ID,
  name: 'Research Village',
  positionX: 5,
  positionY: 5,
  villageResources: { Wood: 500, Stone: 400, Hop: 40, Beer: 200 },
  resourcesPerHour: { Wood: 10, Stone: 5, Hop: 2, Beer: 1 },
  resourceLimit: 1000,
  population: 10,
  populationLeft: 5,
  buildings: [smithBuilding, barracksBuilding],
  availableResearches: [researchSpear],
  completedResearches: [],
  unitsInVillage: [
    { unit: { unitName: 'Spear' }, amount: 0 },
    { unit: { unitName: 'Axe' }, amount: 0 },
  ],
  validBuildPositions: [],
  buildingsThatCanBeBuild: [],
  incomingAttacks: [],
  outgoingAttacks: [],
  returningCombatTravels: [],
  ...overrides,
});

describe('Research building flow with full mock data', () => {
  it('starts research, updates resources, and unlocks a unit', () => {
    cy.log('Stub village list and initial village state');
    cy.intercept('GET', `${API_BASE}/village/`, {
      statusCode: 200,
      body: [{ villageId: VILLAGE_ID, name: 'Research Village', positionX: 5, positionY: 5 }],
    }).as('getVillageList');

    const initialVillage = createVillageState();
    cy.intercept('GET', `${API_BASE}/village/${VILLAGE_ID}`, {
      statusCode: 200,
      body: initialVillage,
    }).as('getVillage');

    const updatedVillage = createVillageState({
      villageResources: { Wood: 400, Stone: 350, Hop: 40, Beer: 200 },
      completedResearches: [researchAxe],
      availableResearches: [researchSpear, researchAxe],
      buildings: [
        {
          ...smithBuilding,
          isResearchInProgress: true,
          currentResearch: researchSpear,
          researchTimeLeft: '00:00:00',
        },
        barracksBuilding,
      ],
    });

    cy.intercept('POST', `${API_BASE}/research/`, (req) => {
      expect(req.body.researchBuildingId).to.eq(SMITH_BUILDING_ID);
      expect(req.body.researchType).to.eq(RESEARCH_IN_PROGRESS);
      req.reply({ statusCode: 200, body: updatedVillage });
    }).as('startResearch');

    cy.log('Visit the village page with mocked auth');
    cy.visit(BASE_URL, {
      onBeforeLoad(win) {
        win.localStorage.setItem('token', 'qa-token');
        win.localStorage.setItem('villageId', `${VILLAGE_ID}`);
      },
    });
    cy.wait(['@getVillageList', '@getVillage']);

    cy.log('Open the research building');
    cy.get('.grid')
      .find('img.tileImg[src*="smith"]')
      .first()
      .click({ force: true });
    cy.get('.smithContainer').should('be.visible');

    cy.log('Start research and wait for the mocked response');
    cy.contains('button', 'Start Research').click();
    cy.wait('@startResearch');

    cy.log('Verify research is shown as in progress');
    cy.contains('Research under progress').should('be.visible');

    cy.log('Verify resources were updated from mock data');
    cy.get('.resourcesContainer #wood p').should('contain', '400');
    cy.get('.resourcesContainer #stone p').should('contain', '350');

    cy.log('Close the research modal');
    cy.get('#modalButton').click({ force: true });

    cy.log('Open barracks to confirm the unlocked unit is available');
    cy.get('[data-testid="building-barracks"]').click({ force: true });
    cy.contains('.unitDescription h1', UNLOCKED_RESEARCH)
      .closest('.unitContainer')
      .parent()
      .should('not.have.class', 'disabledOverlay');
    cy.contains('.unitDescription h1', UNLOCKED_RESEARCH)
      .closest('.unitContainer')
      .parent()
      .within(() => {
        cy.contains('Unit requires research').should('not.exist');
        cy.contains('button', 'Train').should('not.be.disabled');
      });
  });
});
