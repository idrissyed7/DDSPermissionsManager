 describe('Delete Users', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

it('should delete user montesm@test.test', () => {
        cy.visit('/users');

        cy.get(':nth-child(2) > [style="cursor: pointer; text-align: right; padding-right: 0.25rem; width: 1rem;"] > [data-cy="delete-users-icon"]')
        .click();
        
        cy.get('[data-cy="delete-user"]')
        .click();
        
        cy.get('td').should('not.eq', 'montesm@test.test');
        
    });
});