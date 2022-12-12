/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

   
        it('should delete the second topic', () => {
        cy.visit('/topics');

        cy.get('table.main > tbody > tr:nth-child(1) > td:nth-child(2)').then(($name) => {

            const name = $name.text()

            cy.get('table.main > tbody > tr:nth-child(1) > td:nth-child(4) > img')
            .click();

            cy.get('[data-cy="delete-topic"]')
            .click();

            cy.get('td').should('not.eq', name);
        
        });
    });

});
