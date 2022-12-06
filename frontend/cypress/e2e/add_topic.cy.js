/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

   
        it('should add a new topic to group Alpha', () => {
        cy.visit('/topics');

        cy.get('[data-cy="dot-topics"]')
        .click();

        cy.get('[data-cy="add-topic"]')
        .click();

        cy.get('[data-cy="topic-name"]')
        .type("Test Topic Alpha");

        cy.get('[data-cy="group-input"]')
        .type("alpha");

        cy.get('[data-cy="button-add-topic"]')
        .click();

        cy.contains('td', 'Test Topic Alpha')   
        .siblings()
        .contains('td', 'Alpha');               
      
    })

    it('should add a new topic to group Beta', () => {
        cy.visit('/topics');

        cy.get('[data-cy="dot-topics"]')
        .click();

        cy.get('[data-cy="add-topic"]')
        .click();

        cy.get('[data-cy="topic-name"]')
        .type("Test Topic Beta");

        cy.get('[data-cy="group-input"]')
        .type("beta");

        cy.get('[data-cy="button-add-topic"]')
        .click();

        cy.contains('td', 'Test Topic Beta')   
        .siblings()
        .contains('td', 'Beta'); 

    })

});
