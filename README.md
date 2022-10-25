# PermissionsManager

### Supported Databases

| Database     | Versions     | Driver                   | Reference                                                                                                                 |
|--------------|--------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------|
| MySQL Server | 8.0 and 5.7  | com.mysql.cj.jdbc.Driver | [link](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-versions.html)                                            |
| PostgreSQL   | 8.2 or newer | org.postgresql.Driver    | [link](https://jdbc.postgresql.org/documentation/#:~:text=The%20current%20version%20of%20the,(JDBC%204.2)%20and%20above.) |

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
