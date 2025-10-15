describe('Auth flow tests', () => {

    const baseUrl = 'http://localhost:5173'; // pas aan indien nodig
    const registerUrl = `${baseUrl}/register`;
    const loginUrl = `${baseUrl}/login`;

    it('should register a new user successfully', () => {
        cy.visit(registerUrl);

        cy.get('input[name="email"]').type('testuser' + Date.now() + '@mail.com');
        cy.get('input[name="username"]').type('TestUser' + Date.now());
        cy.get('input[name="password"]').type('Test123!');
        cy.get('input[name="confirmPassword"]').type('Test123!');

        cy.get('button[type="submit"]').click();

        // Controleer of redirect naar login of dashboard plaatsvindt
        cy.url().should('include', '/login');
    });

    it('should fail login with wrong username', () => {
        cy.visit(loginUrl);

        cy.get('input[name="username"]').type('wrong@mail.com');
        cy.get('input[name="password"]').type('Test123!');

        cy.get('button[type="submit"]').click();

        cy.contains('Invalid Login Credentials').should('be.visible');
    });

    it('should fail login with wrong password', () => {
        cy.visit(loginUrl);

        cy.get('input[name="username"]').type('test5@mail.com'); // seeded user
        cy.get('input[name="password"]').type('WrongPassword!');

        cy.get('button[type="submit"]').click();

        cy.contains('Invalid Login Credentials').should('be.visible');
    });

    it('should fail login with empty form', () => {
        cy.visit(loginUrl);

        cy.get('button[type="submit"]').click();

        cy.contains('Vul alle velden in').should('be.visible'); // afhankelijk van jouw frontend validatie
    });

});
