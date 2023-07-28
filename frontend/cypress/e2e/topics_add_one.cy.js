// Copyright 2023 DDS Permissions Manager Authors
/// <reference types="Cypress" />

describe('Topics Capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    it('should add a new topic to group Alpha', () => {
        cy.visit('/topics');
        
        cy.get('[data-cy="group-input"]')
        .type("alpha");
        
        cy.wait(500);
        
        cy.get('[data-cy="group-input"]').type('{downArrow}').type('{enter}');

        cy.get('[data-cy="add-topic"]')
        .click();

        cy.get('[data-cy="topic-name"]')
        .type("Test Topic Alpha");

        cy.get('[data-cy="button-add-topic"]')
        .click({force: true});

        cy.get('.header-title')
        .contains('Test Topic Alpha');            
      
    });
});
