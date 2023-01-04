/// <reference types="Cypress" />

describe('Group Capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
   
    it('should add a new group', () => {
        cy.visit('/groups');

        cy.get('[data-cy="add-group"]')
        .click();

        cy.get('[data-cy="group-new-name"]')
        .type("TestGroup");

        cy.get('[data-cy="button-add-group"]')
        .click();

        cy.get('td').should('contain.text', 'TestGroup');	

    });

    it('should edit the name of a group', () => {
        cy.visit('/groups');

        cy.get('[data-cy="edit-group-icon-TestGroup"]')
        .click();

        cy.get('[data-cy="group-new-name"]')
        .clear()
        .type("NewGroup");

        cy.get('[data-cy="edit-group"]')
        .click();

        cy.get('td').should('not.eq', 'TestGroup');

        cy.get('td').contains('NewGroup');
    });

    it('should delete a group', () => {
        cy.visit('/groups');

        cy.get('[data-cy="delete-group-icon-NewGroup"]')
        .click(); 

        cy.get('[data-cy="delete-group"]')
        .click();

        cy.get('td').should('not.eq', 'NewGroup');
        
        });

    it('should navigate to the link', () => {
        cy.visit('/groups');

        cy.get(':nth-child(1) > :nth-child(5) > center')
        .click();
        
        cy.get('.header-title').contains('Users')   

    })
});

