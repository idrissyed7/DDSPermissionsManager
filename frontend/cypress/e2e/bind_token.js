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

        cy.get('textarea').then(($bindToken) => {

            const bindToken = $bindToken.text();

            cy.visit('/topics');

            cy.get('td').contains('Test Topic 123')
            .click();

            cy.get('[data-cy="bind-token-input"]')
            .type(bindToken);

            cy.get('[data-cy="add-application-button"]')
            .click();

            cy.get('span').should('contain.text', 'Application One');
        })
    });
});
