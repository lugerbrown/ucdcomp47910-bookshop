# COMP47910 BookStore - Secure Web Application

This is a web-based bookstore application built with Spring Boot, designed for educational purposes as part of the **University College Dublin Secure Software Engineering module (COMP47910)**. The application serves as a practical platform to explore and implement security best practices in web development.

## Project Overview

The BookStore application is a full-featured e-commerce web shop that includes:
- **User Authentication & Authorization**: User registration, login, and role-based access control
- **Book Management**: CRUD operations for books and authors
- **Shopping Cart & Checkout**: Complete shopping experience with cart management
- **Admin Panel**: Administrative interface for managing books, authors, and users
- **Security Features**: SSL/TLS encryption, secure database connections, and authentication mechanisms

This project demonstrates real-world security challenges and solutions commonly encountered in web application development, making it an ideal learning tool for understanding secure coding practices, authentication systems, and data protection strategies.

## Requirements

### Java Environment
- **Java 17** or higher (JDK)
- **Maven 3.6+** (for building the project)
- **Spring Boot 3.5.0** (included in the project dependencies)

### Container Platform (Required)
You need a container platform to run MySQL and optionally the entire application stack:

#### Docker
- **Docker Desktop**: [Download Docker Desktop](https://www.docker.com/products/docker-desktop/)
- **Docker Compose**: Included with Docker Desktop

#### Rancher Desktop (Alternative to Docker)
- **Rancher Desktop**: [Download Rancher Desktop](https://rancherdesktop.io/)
- Provides Docker-compatible container runtime with Kubernetes support

### Development Tools (Optional)

#### Tilt (Optional - for enhanced development workflow)
- **Tilt**: [Install Tilt](https://docs.tilt.dev/install.html)
- Automates local development environment management
- Provides live reload and monitoring capabilities
- Not required but recommended for active development

### System Requirements
- **Operating System**: Windows 10+, macOS 10.15+, or Linux
- **Memory**: 4GB RAM minimum (8GB recommended)
- **Disk Space**: 2GB free space for containers and dependencies

### Installation Links
- [Java 17 (OpenJDK)](https://openjdk.org/projects/jdk/17/)
- [Maven](https://maven.apache.org/install.html)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Rancher Desktop](https://rancherdesktop.io/)
- [Tilt](https://docs.tilt.dev/install.html)

# Setup

This application requires sensitive information such as database credentials and SSL certificates, which are not included in the repository for security reasons. Instead, these are provided via a `.env` file located in the `docker/` directory. The `.env` file is essential for configuring the database connection and SSL truststore used by the application.

To simplify the setup process, a script named `setup-env.sh` is provided at the root of the project. This script will:
- Prompt you for the necessary database credentials and truststore password.
- Generate the `docker/.env` file with the required environment variables.
- Automatically create the SSL certificates and Java truststore needed for secure MySQL connections.

**Note:** The `.env` file and generated certificates are not tracked by git and must be created locally using the setup script.

To run the setup, execute:
```bash
bash ./setup-env.sh
```
Follow the prompts to enter your credentials and details. After setup, you can start the MySQL server and the application as described below.

### Example `.env` file
Below is an example of what your `docker/.env` file might look like after running the setup script:

```env
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=library
MYSQL_USER=appuser
MYSQL_PASSWORD=appuserpassword
BOOK_STORE_CONNECTION_STRING="jdbc:mysql://localhost:3306/library?serverTimezone=UTC&useSSL=true&requireSSL=true"
SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD=truststorepassword
SPRING_DATASOURCE_HIKARI_TRUSTSTORE_LOCATION=file:/_dev/COMP47910-BookStore/docker/mysql-ssl/truststore.jks
```

# Run MySQL Server in a Docker container

To run MYSQL server only using the configured env variables.

## Running MySQL Server 

``` bash
docker-compose -f docker/docker-compose.yml --env-file docker/.env up -d
```

## Create Admin User

After starting the MySQL server, you need to create an admin user to access the admin features of the application. Execute the provided SQL script:

``` bash
# Connect to the MySQL container and execute the admin user creation script
docker exec -i $(docker ps -q --filter "name=mysql") mysql -u root -p$(grep MYSQL_ROOT_PASSWORD docker/.env | cut -d '=' -f2) $(grep MYSQL_DATABASE docker/.env | cut -d '=' -f2) < scripts/create-admin-user.sql
```

Alternatively, you can connect to MySQL manually and run the script:

``` bash
# Connect to MySQL container
docker exec -it $(docker ps -q --filter "name=mysql") mysql -u root -p

# Then execute the SQL commands from scripts/create-admin-user.sql
```

The script creates an admin user with:
- **Username:** admin
- **Password:** password
- **Role:** ADMIN

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

## Running the Development Environment with Tilt

[Tilt](https://tilt.dev/) is a tool that helps developers manage and automate the process of running and updating multi-service development environments (such as those using Docker Compose or Kubernetes). It watches your code and configuration for changes and automatically updates your running services, making local development faster and easier.

### How to Use Tilt in This Project

You can use Tilt to spin up the entire development environment, including all dependencies, with a single command. This is especially useful for local development and testing.

#### On macOS/Linux (Bash)
Run the following script to load environment variables and start Tilt:

```bash
./run-tilt.sh
```

#### On Windows (PowerShell)
Run the following script to do the same in PowerShell:

```powershell
./run-tilt.ps1
```

Tilt will start and manage your services as defined in the `Tiltfile` and Docker Compose files. You can then access the Tilt web UI (usually at http://localhost:10350) to monitor and control your development environment.

**Note:** Make sure you have [Tilt installed](https://docs.tilt.dev/install.html) on your system before running these scripts.
