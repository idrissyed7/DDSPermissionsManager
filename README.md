# PermissionsManager

### Supported Databases

| Database     | Versions     | Driver                   | Reference                                                                                                                 |
|--------------|--------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------|
| MySQL Server | 8.0 and 5.7  | com.mysql.cj.jdbc.Driver | [link](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-versions.html)                                            |
| PostgreSQL   | 8.2 or newer | org.postgresql.Driver    | [link](https://jdbc.postgresql.org/documentation/#:~:text=The%20current%20version%20of%20the,(JDBC%204.2)%20and%20above.) |

To connect to a database, please set the following environment variables:

* DPM_DATABASE (Options include `POSTGRES` or `MYSQL`)
* DPM_JDBC_URL
* DPM_JDBC_DRIVER (see Driver column for values)
* DPM_JDBC_USER
* DPM_JDBC_PASSWORD
* DPM_AUTO_SCHEMA_GEN (Options include `none`, `create-only`, `drop`, `create`, `create-drop`, `validate`, and `update` (default value))

The following describes the options for DPM_AUTO_SCHEMA_GEN environment variable in detail:

**none** - No action will be performed. 

**create-only** - Database creation will be generated.

**drop** - Database dropping will be generated.

**create** - Database dropping will be generated followed by database creation.

**create-drop** - Drop the schema and recreate it on SessionFactory startup. Additionally, drop the schema on SessionFactory shutdown.

**validate** - Validate the database schema.

**update** - Update the database schema.

### Initial User
Permissions Manager requires an initial super administrator for the purpose of logging in and adding other super admins
or regular users. To do so, please connect to the database which Permissions Manager will connect to and execute the 
following SQL statement:

```sql
INSERT INTO permissions_user (admin, email)
VALUES (true, 'admin-email@GoogleAccount.com');
```
