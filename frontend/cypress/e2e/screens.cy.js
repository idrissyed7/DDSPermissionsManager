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

    // it('should add a new user', () => {
    //     cy.visit('/users');

    //     cy.get('[data-cy="dot-users"]')
    //     .click();

    //     cy.get('[data-cy="add-user"]')
    //     .click();

    //     cy.get('[data-cy="email-input"]')
    //     .type("testuser@email.com");

    //     cy.get('[data-cy="group-input"]')
    //     .type("alpha");

    //     cy.get('[data-cy="button-add-user"]')
    //     .click();

    //     // cy.get('td').should('have.attr', 'value', 'testuser@email.com');	

    // })

    // it('should delete the first user', () => {
    //     cy.visit('/users');

    //     cy.get(':nth-child(2) > [style="width: 2rem;"] > .group-membership-checkbox')
    //     .click();

    //     cy.get('[data-cy="dot-users"]')
    //     .click();

    //     cy.get('[data-cy="delete-user"]')
    //     .click().type('{enter}');
    // })
});
