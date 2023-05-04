/// <reference types="Cypress" />

describe('Applications Capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });

    it('should delete the new application', () => {
        cy.visit('/applications');

        cy.get('td').contains('New Application').siblings().find('[data-cy="delete-application-icon"]')
        .click(); 

        cy.get('[data-cy="delete-application"]')
        .click();

        cy.get('td').should('not.eq', 'New Application');
        
        });

    // it('should copy the first curl command in application details', () => {
    //     cy.visit('/applications');

    //     cy.get('td').contains('Application One')
    //     .click();

    //     cy.get('[data-cy="curl-command-1-copy"]')
    //     .click();

    //     cy.window().then((win) => {
    //         win.navigator.clipboard.readText().then((text) => {
    //           text.should("eq", "curl -c cookies.txt -H'Content-Type: application\/json' -d\"${json}\" ${DPM_URL}/api/login");
    //         });
    //       });
    // });
});
