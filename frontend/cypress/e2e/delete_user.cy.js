/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

   
        it('should delete the first user', () => {
        cy.visit('/users');

        cy.wait(500);

        cy.get('[data-cy="users-table"] > :nth-child(2) > :nth-child(2)').then(($name) => {

            const name = $name.text()

            cy.get(':nth-child(2) > [style="cursor: pointer; text-align: right; padding-right: 0.25rem; width: 1rem;"] > img')
            .click()

            cy.get('[data-cy="delete-user"]')
            .click();

            cy.wait(500);

            cy.get('td').should('not.eq', name);
        
        });
    });

});
