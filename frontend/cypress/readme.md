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

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
