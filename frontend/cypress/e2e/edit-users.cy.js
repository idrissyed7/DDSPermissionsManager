 describe('Users capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
 
 it('should add a new user', () => {
        cy.visit('/users');

        cy.wait(300);

        cy.get('td').contains('kstanley@test.test').siblings().find('[data-cy="edit-users-icon"]')
        .click();

        cy.get(':nth-child(9) > .switch > .slider').click();

        cy.get('[data-cy="save-edit-user"]').click();

        cy.wait(500);

        // cy.get('tbody > :nth-child(1) > :nth-child(6) > center')

        // cy.get('td').contains('kstanley@test.test').siblings().get('td').contains('✓'); //modify to check 2 ✓
        // .should('have.length', 2);
        cy.get('[data-cy="users-table"]').contains('kstanley@test.test').siblings('[data-cy="is-application-admin"]').contains('✓');
        
    });
});