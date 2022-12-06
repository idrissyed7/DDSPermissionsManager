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

        cy.wait(500);

        cy.get(':nth-child(2) > [style="cursor: pointer; width: 20.8rem; line-height: 2.2rem;"]')
        .then(($name) => {

            const name = $name.text()

            cy.get(':nth-child(2) > [style="cursor: pointer; text-align: right; padding-right: 0.25rem;"] > img')
            .click();

            cy.get('[data-cy="delete-application"]')
            .click();

            cy.wait(500);

            cy.get('td').should('not.eq', name);
        
        });
    });

});
