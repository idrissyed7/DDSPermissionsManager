// Copyright 2023 DDS Permissions Manager Authors
/// <reference types="Cypress" />

describe('Bind Token Functionality', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    it('should go into application details and generate a bind token', () => {
        cy.visit('/applications');

        cy.get('td').contains('Application One')
        .click();

        cy.get('[data-cy="generate-bind-token-button"]')
        .click();

        cy.wait(500);

        cy.get('textarea').invoke('val').should('contains', 'eyJh');
        });
});
 

