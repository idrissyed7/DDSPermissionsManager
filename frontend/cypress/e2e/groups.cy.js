/// <reference types="Cypress" />

describe('Group Capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
   
    it('should add a new group', () => {
        cy.visit('/groups');

        cy.get('[data-cy="dot-groups"]')
        .click();

        cy.get('[data-cy="add-group"]')
        .click();

        cy.get('[data-cy="group-new-name"]')
        .type("Test Group");

        cy.get('[data-cy="button-add-group"]')
        .click();

        cy.get('td').should('contain.text', 'Test Group');	

    });

    it('should edit the name of a group', () => {
        cy.visit('/groups');

        cy.get('td').contains('Test Group').siblings().find('[data-cy="edit-group-icon"]')
        .click();

        cy.get('[data-cy="group-new-name"]')
        .clear()
        .type("New Group");

        cy.get('[data-cy="edit-group"]')
        .click();

        cy.get('td').should('not.eq', 'Test Group');

        cy.get('td').contains('New Group');
    });

    it('should delete a group', () => {
        cy.visit('/groups');

        cy.get('td').contains('New Group').siblings().find('[data-cy="delete-group-icon"]')
        .click(); 

        cy.get('[data-cy="delete-group"]')
        .click();

        cy.get('td').should('not.eq', 'New Group');
        
        });
});

