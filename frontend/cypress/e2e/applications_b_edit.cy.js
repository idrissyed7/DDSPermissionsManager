// Copyright 2023 DDS Permissions Manager Authors
/// <reference types="Cypress" />

describe('Applications Capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
   
    it('should edit the name of the second application', () => {
        cy.visit('/applications');

        cy.get('td').contains('Test Application')
        .click();

        cy.wait(500);
        
        cy.get('[data-cy="edit-application-icon"]')
        .click({force: true});

        cy.get('[data-cy="application-name"]')
        .clear()
        .type("New Application");

        cy.get('[data-cy="save-application"]')
        .click();

        cy.visit('/applications');

        cy.get('td').should('not.eq', 'Test Application');

        cy.get('td').contains('New Application');

        });
});
