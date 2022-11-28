/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

   
        it('should add a new topic', () => {
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

    })

});
