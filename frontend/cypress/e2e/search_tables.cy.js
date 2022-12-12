/// <reference types="Cypress" />

describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    // afterEach(() => {
    //     cy.visit('http://localhost:8080/api/logout')
    // });

    it('should filter Users correctly', () => {
        cy.visit('/users');
        cy.get('[data-cy="search-users-table"]').type('unity');
        cy.wait(500);

        cy.get('[data-cy="users-table"] tr')
        .should('have.length', 2);
    })
       

    it('should filter Super Users correctly', () => {
        cy.visit('/users');
        cy.get('[data-cy="search-super-users-table"]').type('computing');

        cy.get('[data-cy="super-users-table"] tr').should('have.length', 3);
    });

    it('should filter Topics correctly', () => {
        cy.visit('/topics');
        cy.get('[data-cy="search-topics-table"]').type('123');

        cy.get('[data-cy="topics-table"] tr')
        .should('have.length', 2);
    });

    it('should filter Applications correctly', () => {
        cy.visit('/applications');
        cy.get('[data-cy="search-applications-table"]').type('two');

        cy.get('[data-cy="applications-table"] tr')
        .should('have.length', 2);
    });

    it('should filter Groups correctly', () => {
        cy.visit('/groups');
        cy.get('[data-cy="search-groups-table"]').type('delta');

        cy.get('[data-cy="groups-table"] tr')
        .should('have.length', 2);
    });
    
});
