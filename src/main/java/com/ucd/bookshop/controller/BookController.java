package com.ucd.bookshop.controller;

import com.ucd.bookshop.exception.BookNotFoundException;
import com.ucd.bookshop.model.Book;
import com.ucd.bookshop.repository.BookRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // Get All Books
    @GetMapping
    public List<Book> getAllBooks(){
        return bookRepository.findAll();
    }

    // Create a new Book
    @PostMapping
    public Book newBook(@Valid @RequestBody Book newBook)
    {
        return bookRepository.save(newBook);
    }

    // Get a Single Book
    @GetMapping("/{id}")
    public Book getBookById(@PathVariable(value = "id") Long bookId) throws BookNotFoundException {
        return bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));}

    // Update an Existing Book
    @PutMapping("/{id}")
    public Book updateBook(@PathVariable(value="id") Long bookId, @Valid @RequestBody Book bookDetails)
            throws BookNotFoundException{
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        book.setBook_name(bookDetails.getBook_name());
        book.setAuthors(bookDetails.getAuthors());
        book.setIsbn(bookDetails.getIsbn());
        return bookRepository.save(book);
    }

    // Delete a Book
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable(value="id") Long bookId) throws BookNotFoundException {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        bookRepository.delete(book);
        return ResponseEntity.ok().build();
    }

}
