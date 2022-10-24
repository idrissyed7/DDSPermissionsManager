# PermissionsManager

### Supported Databases

| Database     | Versions         | Driver                   |
|--------------|------------------|--------------------------|
| MySQL Server | 8.0, 5.7 and 5.6 | com.mysql.cj.jdbc.Driver |
| PostgreSQL   | 8.2 or newer     | org.postgresql.Driver    |

To connect to a database, please set the following environment variables:

* JDBC_URL
* JDBC_DRIVER (see Driver column for values)
* JDBC_USER
* JDBC_PASSWORD

### Initial User
Permissions Manager requires an initial super administrator for the purpose of logging in and adding other super admins
or regular users. To do so, please connect to the database which Permissions Manager will connect to and execute the 
following SQL statement:

```sql
INSERT INTO permissions_user (admin, email)
VALUES (true, 'admin-email@GoogleAccount.com');
```
