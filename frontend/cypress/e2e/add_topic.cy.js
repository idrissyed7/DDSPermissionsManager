/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

   
        it('should add a new topic', () => {
        cy.visit('/topics');

        cy.get('[data-cy="dot-topics"]')
        .click();

        cy.get('[data-cy="add-topic"]')
        .click();

        cy.get('[data-cy="topic-name"]')
        .type("Test Topic");

        cy.get('[data-cy="group-input"]')
        .type("alpha");

        cy.get('[data-cy="button-add-topic"]')
        .click();

        cy.get('td').should('contain.text', 'Test Topic');	

    })

});
