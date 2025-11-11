describe('Auth flow tests', () => {

    const baseUrl = 'http://localhost:8081';
    const registerUrl = `${baseUrl}/register`;
    const loginUrl = `${baseUrl}/login`;

    beforeEach(() => {
        cy.visit(baseUrl)
    });

    it('should register a new user successfully', () => {
        cy.visit(registerUrl);

        cy.get('input[type="email"]').type('testuser' + Date.now() + '@mail.com');
        cy.get('input[placeholder="Username"]').type('TestUser' + Date.now());
        cy.get('input[placeholder="Password"]').type('Test123!');
        cy.get('input[placeholder="Repeat password"]').type('Test123!');

        cy.get('button[class="submitButton"]').click();

        // Controleer of redirect naar login of dashboard plaatsvindt
        cy.url().should('include', '/login');
    });

    it('should fail login with wrong username', () => {
        cy.visit(loginUrl);

        cy.get('input[placeholder="Username"]').type('wrong@mail.com');
        cy.get('input[placeholder="Password"]').type('Test123!');

        cy.get('button[class="submitButton"]').click();

        cy.contains('Something went wrong').should('be.visible');
    });

    it('should fail login with wrong password', () => {
        cy.visit(loginUrl);

        cy.get('input[placeholder="Username"]').type('test5@mail.com'); // seeded user
        cy.get('input[placeholder="Password"]').type('WrongPassword!');

        cy.get('button[class="submitButton"]').click();

        cy.contains('Something went wrong').should('be.visible');
    });

    it('should fail login with empty form', () => {
        cy.visit(loginUrl);

        cy.get('button[class="submitButton"]').click();

        cy.contains('Username is required').should('be.visible');
        cy.contains('Password is required').should('be.visible');
    });

});
