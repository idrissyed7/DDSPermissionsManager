/// <reference types="Cypress" />

describe('Applications Capabilities', () => {
    beforeEach(() => {
        cy.login('unity-admin', 'password');
        cy.intercept('http://localhost:8080/api/token_info').as('tokenInfo');
        cy.visit('http://localhost:8080/');
        cy.wait('@tokenInfo');
    });
   
    it('should add a new application', () => {
        cy.visit('/applications');

        cy.get('[data-cy="group-input"]')
        .type("alpha");
        
        cy.wait(500);
        
        cy.get('[data-cy="group-input"]').type('{downArrow}').type('{enter}');
        
        cy.get('[data-cy="add-application"]')
        .click();

        cy.get('[data-cy="application-name"]')
        .type("Test Application");

        cy.get('[data-cy="button-add-application"]')
        .click();

        cy.get('td').should('contain.text', 'Test Application');	
    });

    it('should edit the name of the second application', () => {
        cy.visit('/applications');

        cy.get('td').contains('Test Application')
        .click();
        
        cy.get('[data-cy="edit-application-icon"]')
        .click({force: true});

        cy.get('[data-cy="application-name"]')
        .clear()
        .type("New Application");

        cy.get('[data-cy="save-application"]')
        .click();

        cy.visit('/applications');

        cy.get('td').should('not.eq', 'Test Application');

        cy.get('td').contains('New Application');

        });

    it('should delete the new application', () => {
        cy.visit('/applications');

        cy.get('td').contains('New Application').siblings().find('[data-cy="delete-application-icon"]')
        .click(); 

        cy.get('[data-cy="delete-application"]')
        .click();

        cy.get('td').should('not.eq', 'New Application');
        
        });

    it('should copy the first curl command in application details', () => {
        cy.visit('/applications');

        cy.get('td').contains('Application One')
        .click();

        cy.get('[data-cy="curl-command-1-copy"]')
        .click();

        cy.window().then((win) => {
            win.navigator.clipboard.readText().then((text) => {
              text.should("eq", "curl -c cookies.txt -H'Content-Type: application\/json' -d\"${json}\" ${DPM_URL}/api/login");
            });
          });

    });
});
