// Copyright 2023 DDS Permissions Manager Authors
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

        cy.get('[data-cy="group-input"]')
        .type("alpha");
        
        cy.wait(500);
        
        cy.get('[data-cy="group-input"]').type('{downArrow}').type('{enter}');
        
        cy.get('[data-cy="add-application"]')
        .click();

        cy.get('[data-cy="application-name"]')
        .type("Test Application");

        cy.get('[data-cy="button-add-application"]')
        .click();

        cy.get('td').should('contain.text', 'Test Application');	
    });
});
