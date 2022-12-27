 describe('Delete Users', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

it('should delete montesm@test.test', () => {
        cy.visit('/users');

        cy.wait(1200);
        
        cy.get('td').contains('montesm@test.test').siblings().find('[data-cy="delete-users-icon"]')
        .click();
        
        cy.get('[data-cy="delete-user"]')
        .click();
        
        cy.wait(1200);

        cy.get('td').should('not.eq', 'montesm@test.test');
        
    });
});