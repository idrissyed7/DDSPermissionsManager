/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

   
        it('should add a new super user', () => {
        cy.visit('/users');

        cy.get('[data-cy="dot-super-users"]')
        .click();

        cy.get('[data-cy="add-super-user"]')
        .click();

        cy.get('[data-cy="email-input"]')
        .type("testuser@email.com");

        cy.get('[data-cy="button-add-super-user"]')
        .click();

        cy.get('td').should('contain.text', 'testuser@email.com');	

    })

});
