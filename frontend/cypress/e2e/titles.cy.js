describe('title should say Permissions Manager', () => {
    beforeEach(() => {
        cy.visit('http://localhost:3000');
    });

    it('should have correct title', () => {
        cy.get('title')
        .invoke('text')
        .should('equal', 'Permissions Manager');
    });

    it('should have correct header', () => {
        cy.get('h1')
        .invoke('text')
        .should('equal', 'Permissions Manager');
    })
    
});