// Copyright 2023 DDS Permissions Manager Authors
/// <reference types="Cypress" />

describe('Group Context capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    it('should activate a new group context', () => {

        cy.get('[data-cy="activate-group-context0"]')
        .click();

        cy.get('[data-cy="group-input"]').should('have.value', 'Alpha');

    });
});