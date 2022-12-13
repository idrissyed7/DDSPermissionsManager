/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

   
        it('should delete the first application', () => {
        cy.visit('/applications');

        cy.get('table.main-table > tbody > tr:nth-child(1) > td:nth-child(2)').then(($name) => {

            const name = $name.text()

            cy.get('table.main-table > tbody > tr:nth-child(1) > td:nth-child(5) > img')
            .click();

            cy.get('[data-cy="delete-application"]')
            .click();

            cy.get('td').should('not.eq', name);
        
        });
    });

});
