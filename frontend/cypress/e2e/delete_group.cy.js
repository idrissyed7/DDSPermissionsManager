/// <reference types="Cypress" />

describe('should manage group capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    after(() => {
        cy.visit('http://localhost:8080/api/logout')
    });

    
    it('should delete a group', () => {
        cy.visit('/groups');

        cy.get('[data-cy="groups-table"] > tbody > tr:nth-child(1) > td:nth-child(2)').then(($name) => {

            const name = $name.text()

            cy.get('[data-cy="groups-table"] > tbody > tr:nth-child(1) > td:nth-child(7)')
            .click();

            cy.get('[data-cy="delete-group"]')
            .click();

            cy.get('td').should('not.eq', name);
        
        });

    });
});