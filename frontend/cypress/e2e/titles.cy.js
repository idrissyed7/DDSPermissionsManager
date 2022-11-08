describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    afterEach(() => {
        cy.visit('http://localhost:8080/api/logout')
    });

    it('should have correct title', () => {
        cy.get('title')
        .invoke('text')
        .should('equal', 'DDS Permissions Manager');
    });

    it('should have correct h1', () => {
        cy.get('h1')
        .invoke('text')
        .should('equal', 'Welcome to the DDS Permissions Manager!');
    });
});
