/// <reference types="Cypress" />

describe('Applications Capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
   
    it('should add a new application', () => {
        cy.visit('/applications');

        cy.get('[data-cy="add-application"]')
        .click();

        cy.get('[data-cy="application-name"]')
        .type("Test Application");

        cy.get('[data-cy="group-input"]')
        .type("alpha");

        cy.get('[data-cy="button-add-application"]')
        .click();

        cy.get('td').should('contain.text', 'Test Application');	
    });

    it('should edit the name of the second application', () => {
        cy.visit('/applications');

        cy.get('td').contains('Test Application').siblings().find('[data-cy="edit-application-icon"]')
        .click();

        cy.get('[data-cy="application-name"]')
        .clear()
        .type("New Application");

        cy.get('[data-cy="save-application"]')
        .click();

        cy.get('td').should('not.eq', 'Test Application');

        cy.get('td').contains('New Application');

        });

    it('should delete the new application', () => {
        cy.visit('/applications');

        cy.get('td').contains('New Application').siblings().find('[data-cy="delete-application-icon"]')
        .click(); 

        cy.get('[data-cy="delete-application"]')
        .click();

        cy.get('td').should('not.eq', 'New Application');
        
        });
});
