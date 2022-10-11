describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.visit('http://localhost:8080/', {
            auth: {
              username: 'unity-admin',
              password: 'password',
            },
        });
        cy.wait(1000);
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
    })
    
});