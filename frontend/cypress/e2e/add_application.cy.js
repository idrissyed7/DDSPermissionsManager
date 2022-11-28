/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

   
        it('should add a new application', () => {
        cy.visit('/applications');

        cy.get('[data-cy="dot-applications"]')
        .click();

        cy.get('[data-cy="add-application"]')
        .click();

        cy.get('[data-cy="application-name"]')
        .type("Test Application");

        cy.get('[data-cy="group-input"]')
        .type("alpha");

        cy.get('[data-cy="button-add-application"]')
        .click();

        cy.get('td').should('contain.text', 'Test Application');	


    })

});
