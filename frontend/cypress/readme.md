Testing Framework "Cypress.io"
https://www.cypress.io/

To open Cypress.io Interface, in the command line and located within 'frontend' folder type: npx cypress open
Select E2E Testing >> Google Chrome >> Start E2E Testing in Chrome.

This will open up a new window with all the E2E specs (located inside cypress/e2e folder)
When you click on the test it should automatically run.

In order for the tests to run in the proper environment, the backend has to be running as well as the frontend.

To start the backend (within 'app' directory): ./gradlew run -t
To start the frontend (within 'frontend' directory): npm run dev

Copyright 2023 DDS Permissions Manager Authors
