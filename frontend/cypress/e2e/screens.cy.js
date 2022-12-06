/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    afterEach(() => {
        cy.visit('http://localhost:8080/api/logout')
    });

    

    it('should have correct title', () => {
        cy.get('title')
        .invoke('text')
        .should('equal', 'DDS Permissions Manager');
        
    });

    it('should have correct h1', () => {
        cy.get('h1')
        .invoke('text')
        .should('equal', 'Welcome to the DDS Permissions Manager!');
        
    });

    it('should load Users/Super Users screen', () => {
        cy.visit('/users');

        cy.get('[data-cy="users"]')
        .invoke('text')
        .should('equal', 'Users');

        cy.get('[data-cy="super-users"]')
        .invoke('text')
        .should('equal', 'Super Users');
    });

    it('should load Topics screen', () => {
        cy.visit('/topics');

        cy.get('[data-cy="topics"]')
        .invoke('text')
        .should('equal', 'Topics');
    });

    it('should load Applications screen', () => {
        cy.visit('/applications');

        cy.get('[data-cy="applications"]')
        .invoke('text')
        .should('equal', 'Applications');
    });

    it('should load Groups screen', () => {
        cy.visit('/groups');

        cy.get('[data-cy="groups"]')
        .invoke('text')
        .should('equal', 'Groups');
    });

});
