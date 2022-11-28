 describe('title should say DDS Permissions Manager', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
 
 it('should add a new user', () => {
        cy.visit('/users');

        cy.get('[data-cy="dot-users"]')
        .click();

        cy.get('[data-cy="add-user"]')
        .click();

        cy.get('[data-cy="email-input"]')
        .type("user@email.com");

        cy.get('[data-cy="group-input"]')
        .type("alpha");

        cy.get('[data-cy="button-add-user"]')
        .click();

        cy.get('td').should('contain.text', 'user@email.com');	

    })

});