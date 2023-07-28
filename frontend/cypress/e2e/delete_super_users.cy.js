// Copyright 2023 DDS Permissions Manager Authors
/// <reference types="Cypress" />

describe('Delete super users', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    it('should delete pasumarthis@test.test.com', () => {
        cy.visit('/users');

        cy.wait(500);

        cy.get('td').contains('pasumarthis@test.test.com').siblings().find('[data-cy="delete-super-users-icon"]')
        .click();

        cy.get('[data-cy="delete-super-user"]')
        .click();

        cy.get('td').should('not.eq', 'pasumarthis@test.test.com');
        
    });
});
