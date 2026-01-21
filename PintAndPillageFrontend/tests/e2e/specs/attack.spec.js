const BASE_URL = 'http://localhost:8081';
const API_BASE = `${BASE_URL}/api`;
const VILLAGE_ID = 1;
const PLAYER_USER_ID = 1;
const ENEMY_USER_ID = 2;

// --- MOCK DATA AND STATE HELPERS ---

const vikingBlueprint = {
    unit: {
        unitName: 'Axe',
        description: 'A fearsome raider, ready to plunder.',
    },
    amount: 50
};

const transportShipBlueprint = {
    unit: {
        unitName: 'TransportShip',
        description: 'A vessel to carry your troops across the seas.',
        shipCapacity: 20,
    },
    amount: 10
};

const createVillageState = (overrides = {}) => {
  const baseVillageTemplate = {
    villageId: VILLAGE_ID,
    villageOwnerId: PLAYER_USER_ID,
    name: 'Attacker Village',
    villageResources: { Wood: 500, Stone: 400, Hop: 40, Beer: 200 },
    unitsInVillage: [
      vikingBlueprint,
      transportShipBlueprint,
    ],
    outgoingAttacks: [],
  };
  return { ...baseVillageTemplate, ...overrides };
};

const createWorldMapState = () => {
    const size = 20;
    const tiles = Array.from({ length: size }, (_, x) => 
        Array.from({ length: size }, (_, y) => ({ tileType: 'Water', x, y }))
    );
    tiles[5][5] = { tileType: 'Grass', x: 5, y: 5 };
    tiles[6][6] = { tileType: 'Grass', x: 6, y: 6 };

    return {
        worldTiles: tiles,
        villages: [
            { villageId: VILLAGE_ID, name: 'Attacker Village', userId: PLAYER_USER_ID, position: { x: 5, y: 5 } },
            { villageId: 99, name: 'Hostile Village', userId: ENEMY_USER_ID, villageOwnerName: 'Enemy', points: 100, position: { x: 6, y: 6 } },
        ],
    };
};

// --- CYPRESS HELPER FUNCTIONS ---

const stubVillageLifecycle = (initialState) => {
  cy.intercept('GET', `${API_BASE}/village/`, {
    statusCode: 200,
    body: [{ villageId: VILLAGE_ID, name: initialState.name, positionX: 5, positionY: 5 }],
  }).as('getVillageList');
  cy.intercept('GET', `${API_BASE}/village/${VILLAGE_ID}`, {
    statusCode: 200,
    body: initialState,
  }).as('getVillage');
};

const visitInitialScreen = () => {
  cy.visit(BASE_URL, {
    onBeforeLoad(win) {
      win.localStorage.setItem('token', 'qa-token');
      win.localStorage.setItem('villageId', `${VILLAGE_ID}`);
    },
  });
  cy.wait(['@getVillageList', '@getVillage']);
};

// --- THE ACTUAL TEST SUITE ---

describe('User Story 25: Attack Hostile Village', () => {

  it('allows a user to attack a hostile village after navigating to the world map', () => {
    // 1. Define initial state and stub APIs
    const initialVillage = createVillageState();
    const worldMapData = createWorldMapState();
    stubVillageLifecycle(initialVillage);

    cy.intercept('GET', `${API_BASE}/world`, { body: worldMapData }).as('getWorldMap');
    cy.intercept('POST', `${API_BASE}/combat`, { body: { message: 'Attack successful!' } }).as('attackRequest');

    // 2. Visit the page and navigate
    visitInitialScreen();
    cy.get('.mapButton').click();
    cy.wait('@getWorldMap');
    cy.url().should('include', '/world');

    // 3. Perform actions on the world map
    cy.get('.villageTile').last().click();

    cy.get('.pillageButton').should('be.visible').click();

    cy.get('.combatModal').should('be.visible');
    cy.get('.combatModal').find('h1').contains('Select your Long Ships');

    cy.get('.combatModal')
      .find('input[type="number"]')
      .first()
      .clear()
      .type('5')
      .blur()
      .should('have.value', '5');

    cy.get('.combatModal')
      .contains('Capacity: 100')
      .should('be.visible');

    cy.get('.combatModal')
      .contains('button', 'Select Units')
      .click({ force: true });
    cy.get('.combatModal').then(($modal) => {
      $modal[0].__vue__.openUnitsFrame();
    });

    cy.get('.combatModal').find('h1').contains('Select your raiders');
    
    cy.get('.combatModal')
      .find('input[type="number"]')
      .first()
      .clear()
      .type('10')
      .blur()
      .should('have.value', '10');

    cy.get('.combatModal').contains('button', 'To Battle!').click();

    // 4. Assertions
    cy.wait('@attackRequest');
    cy.get('.v-toaster').should('be.visible').and('contain.text', 'Units Send!');
  });
});
