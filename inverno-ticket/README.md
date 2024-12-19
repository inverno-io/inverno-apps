# Inverno Ticket application

The Inverno Ticket application is a simple yet powerful ticket management application that can be used to organize and plan tasks for a single person project.

The repository provides five branches:

- **master** which defines the code basline for the application.
- **secure** which extends the **master** branch with basic security: authentication and identity.
- **rbac** which extends the **secure** branch with role-based access control.
- **pbac** which extends the **secure** branch with permission-based access control.
- **ldap** which extends the **rbac** branch with LDAP authentication.

## Start/Stop the application

Create containers and start the application:

```
$ docker-compose up -d
```

Stop the application:

```
$ docker-compose stop
```

Start the application:

```
$ docker-compose start
```

Stop the application and remove containers:

```
$ docker-compose down
```

Stop the application and remove containers and volumes:

```
$ docker-compose down -v
```

Connect to Redis using Redis CLI:

```
$ docker run -it --network inverno-ticket_default --rm redis redis-cli -h redis
```

## Create volumes

```
> docker volume create inverno-ticket_logs
> docker volume create inverno-ticket_data
```
