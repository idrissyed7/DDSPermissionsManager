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

        cy.wait(500);

        cy.get(':nth-child(3) > [style="line-height: 2.2rem; cursor: pointer; width: 20.8rem;"]').then(($name) => {

            const name = $name.text()

            cy.get(':nth-child(3) > [style="cursor: pointer; text-align: right; padding-right: 0.25rem;"] > img')
            .click();

            cy.get('[data-cy="delete-topic"]')
            .click();

            cy.wait(500);

            cy.get('td').should('not.eq', name);
        
        });
    });

});
