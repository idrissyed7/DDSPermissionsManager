// Copyright 2023 DDS Permissions Manager Authors
/// <reference types="Cypress" />

describe('Topics Capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    it('should delete the Test Topic Beta', () => {
        cy.visit('/topics');

        cy.get('td').contains('Test Topic Beta').siblings().find('[data-cy="delete-topic-icon"]')
        .click();

        cy.get('[data-cy="delete-topic"]')
        .click();

        cy.get('td').should('not.eq', 'Test Topic Beta');
        
    });
});
