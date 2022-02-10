# Inverno Ticket application

The Inverno Ticket application is a simple yet powerful ticket management application that can be used to organize and plan tasks for a single person project.

## Start/Stop the application

Create containers and start the application:

```
> docker-compose up -d
```

Stop the application:

```
> docker-compose stop
```

Start the application:

```
> docker-compose start
```

Stop the application and remove containers:

```
> docker-compose down
```

Stop the application and remove containers and volumes:

```
> docker-compose down -v
```

## Create volumes

```
> docker volume create inverno-ticket_logs
> docker volume create inverno-ticket_data
```