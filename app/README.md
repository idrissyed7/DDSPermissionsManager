## Permissions Manager

The project requires JDK 11 or later to build and run. There is no need to
install Micronaut, or Gradle, or any tools other than the JDK in order to
build and run the app.

The app may be run locally using gradle:

    ./gradlew run

To support automatic restart to support changes made while the app is running:

    ./gradlew run -t

To build a standalone executable `.jar` file:

    ./gradlew assemble

That will generate `build/libs/permissionsManager-[version number]-all.jar` which
may be executed as a normal executable jar:

```
java -jar build/libs/permissionsManager-0.1-all.jar 
 __  __ _                                  _   
|  \/  (_) ___ _ __ ___  _ __   __ _ _   _| |_ 
| |\/| | |/ __| '__/ _ \| '_ \ / _` | | | | __|
| |  | | | (__| | | (_) | | | | (_| | |_| | |_ 
|_|  |_|_|\___|_|  \___/|_| |_|\__,_|\__,_|\__|
  Micronaut (v3.4.4)

09:28:40.359 [main] INFO  org.hibernate.Version - HHH000412: Hibernate ORM core version [WORKING]
09:28:40.424 [main] INFO  o.h.annotations.common.Version - HCANN000001: Hibernate Commons Annotations {5.1.2.Final}
09:28:40.540 [main] INFO  org.hibernate.dialect.Dialect - HHH000400: Using dialect: org.hibernate.dialect.H2Dialect
09:28:41.331 [main] INFO  io.micronaut.runtime.Micronaut - Startup completed in 1381ms. Server Running: http://localhost:8080

```
