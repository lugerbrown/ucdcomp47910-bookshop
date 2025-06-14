# Run MySQL Server in a Docker container

To run MYSQL server only using the configured env variables.

## Running MySQL Server 
``` bash
docker-compose -f docker/docker-compose.yml --env-file docker/.env up -d
```
## Run the application
build and run dependencies and the app using the docker-compose from both dependencies and the application

``` bash
docker-compose -f docker/docker-compose.yml -f docker/docker-compose-app.yml --env-file docker/.env up -d

## Book API

here are the curl commands to test the server

this creates a book
``` bash
curl -d '{"book_name": "Book1","author_name": "Author1","isbn":"ISBN1"}' -H 'Content-Type: application/json' http://localhost:8080/books
```

this gets all books
``` bash
curl -H 'Content-Type: application/json' http://localhost:8080/books
```
or you can use the browser to access the URL http://localhost:8080/books

to modify a book
``` bash
curl -X PUT -H "Content-Type: application/json" -d '{"book_name": "New_Book","author_name": "Author2","isbn":"ISBN2"}' http://localhost:8080/books/2
```
then to delete a book
``` bash
curl -X DELETE http://localhost:8080/books/2
```

## Running the Application with run-app.sh

You can also run the Spring Boot application directly from the command line using the provided `run-app.sh` script. This script automatically loads environment variables from the `docker/.env` file before starting the application, ensuring that all necessary configuration (such as database connection details) is available to the app.

To use the script, run:

```bash
./run-app.sh
```

**Note:** You still need to start the MySQL server using Docker Compose before running the application. The application depends on the MySQL server being up and running. To start the MySQL server, use:

```bash
docker-compose -f docker/docker-compose.yml --env-file docker/.env up -d
```

### Why use the .env file?

The `.env` file (located in the `docker/` directory) contains environment variables such as database credentials and configuration. The `run-app.sh` script loads these variables into your shell environment before starting the application, so the app can connect to the MySQL server and use the correct settings. This approach keeps sensitive information out of the codebase and makes it easy to change configuration without modifying code.

## Running the Application on Windows (PowerShell)

For Windows users, you can use PowerShell to achieve the same result as the `run-app.sh` script. Here is a PowerShell script that loads environment variables from `docker/.env` and runs the Spring Boot application:

Create a file named `run-app.ps1` with the following content:

```powershell
# This script loads environment variables from docker/.env and runs the Spring Boot application
Get-Content docker/.env | ForEach-Object {
    if ($_ -match '^(.*?)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}
./mvnw spring-boot:run
```

To run the script, open PowerShell and execute:

```powershell
./run-app.ps1
```

**Note:** As with the Bash script, you still need to start the MySQL server using Docker Compose before running the application:

```powershell
docker-compose -f docker/docker-compose.yml --env-file docker/.env up -d
```

This ensures the application has the required environment variables and can connect to the MySQL server.

  