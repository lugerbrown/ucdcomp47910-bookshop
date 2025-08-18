package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Author;
import com.ucd.bookshop.model.Book;
import com.ucd.bookshop.repository.AuthorRepository;
import com.ucd.bookshop.repository.BookRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@Controller
@RequestMapping("/books")
public class BookWebController {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookWebController(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    @GetMapping
    public String listBooks(Model model) {
        model.addAttribute("books", bookRepository.findAll());
        return "books";
    }

    @GetMapping("/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("allAuthors", authorRepository.findAll());
        return "book-form";
    }

    @PostMapping("/add")
    public String addBook(@ModelAttribute Book book, @RequestParam(value = "authorIds", required = false) Long[] authorIds) {
        if (authorIds != null) {
            Set<Author> authors = new HashSet<>();
            for (Long authorId : authorIds) {
                authorRepository.findById(authorId).ifPresent(authors::add);
            }
            book.setAuthors(authors);
        }
    bookRepository.save(book);
        return "redirect:/books";
    }

    @GetMapping("/edit/{id}")
    public String showEditBookForm(@PathVariable Long id, Model model) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) return "redirect:/books";
        model.addAttribute("book", book);
        model.addAttribute("allAuthors", authorRepository.findAll());
        return "book-form";
    }

    @PostMapping("/edit/{id}")
    public String editBook(@PathVariable Long id, @ModelAttribute Book book, @RequestParam(value = "authorIds", required = false) Long[] authorIds) {
        book.setId(id);

        // Handle authors - only existing authors by ID
        if (authorIds != null) {
            Set<Author> authors = new HashSet<>();
            for (Long authorId : authorIds) {
                authorRepository.findById(authorId).ifPresent(authors::add);
            }
            book.setAuthors(authors);
        } else {
            book.setAuthors(new HashSet<>());
        }

    bookRepository.save(book);
        return "redirect:/books";
    }

    @PostMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookRepository.deleteById(id);
        return "redirect:/books";
    }
}
