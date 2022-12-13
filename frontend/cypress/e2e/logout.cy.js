/// <reference types="Cypress" />

describe('should logout of the app', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
   
    it('should logout', () => {
        cy.get('[data-cy="avatar-dropdown"]').click();
        
        cy.get('[data-cy="logout-button"]').click();
        
        cy.get('.login-button span a')
        .invoke('text')
        .should('equal', 'Login with Google');
    });

    
});
