 describe('Users capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
 
 it('should edit the user', () => {
        cy.visit('/users');

        cy.wait(1200);

        cy.get('td').contains('kstanley@test.test').siblings().find('[data-cy="edit-users-icon"]')
        .click();

        cy.get(':nth-child(8) > .switch > .slider').click();

        cy.get('[data-cy="save-edit-user"]').click();

        cy.wait(700);

        cy.get('[data-cy="users-table"]').contains('kstanley@test.test').siblings('[data-cy="is-application-admin"]').contains('✓');
        
    });
});