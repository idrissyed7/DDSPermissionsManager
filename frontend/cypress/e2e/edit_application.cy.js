/// <reference types="Cypress" />

describe('should manage application capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    after(() => {
        cy.visit('http://localhost:8080/api/logout')
    });

    
    it('should edit the name of the second application', () => {
        cy.visit('/applications');

        cy.get('[data-cy="applications-table"] > tbody > tr:nth-child(2) > td:nth-child(2)').then(($name) => {

            const name = $name.text()

            cy.get('[data-cy="applications-table"] > tbody > tr:nth-child(1) > td:nth-child(4) > img')
            .click();

            cy.get('[data-cy="application-name"]')
            .clear()
            .type("New Application");

            cy.get('[data-cy="save-application"]')
            .click();

            cy.get('td').should('not.eq', name);

            cy.get('td').contains('New Application');

        });

    });
});