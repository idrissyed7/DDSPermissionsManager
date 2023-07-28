// Copyright 2023 DDS Permissions Manager Authors
/// <reference types="Cypress" />

describe('Super users capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
   
    it('should add a new super user', () => {
        cy.visit('/users');

        cy.get('[data-cy="add-super-user"]')
        .click();

        cy.get('[data-cy="email-input"]')
        .type("superuser@email.com");

        cy.get('[data-cy="button-add-super-user"]')
        .click();

        cy.get('td').should('contain.text', 'superuser@email.com');	
    });
});
