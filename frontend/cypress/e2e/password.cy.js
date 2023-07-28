// Copyright 2023 DDS Permissions Manager Authors
/// <reference types="Cypress" />

describe('should generate a password', () => {
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

        cy.get('[data-cy="generate-password-button"]')
        .click();

        cy.get('[data-cy="generated-password"]').should('not.be.empty');
        
    });
});
