# Run MySQL Server in a Docker container

To run MYSQL server only using the configured env variables.

``` bash
docker-compose --env-file .env up -d
```


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

  