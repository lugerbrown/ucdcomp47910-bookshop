package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Book;
import com.ucd.bookshop.repository.BookRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@Controller
@RequestMapping("/books")
public class BookWebController {
    private final BookRepository bookRepository;

    @GetMapping
    public String listBooks(Model model) {
        model.addAttribute("books", bookRepository.findAll());
        return "books";
    }

    @GetMapping("/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        return "book-form";
    }

    @PostMapping("/add")
    public String addBook(@ModelAttribute Book book) {
        bookRepository.save(book);
        return "redirect:/books";
    }

    @GetMapping("/edit/{id}")
    public String showEditBookForm(@PathVariable Long id, Model model) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) return "redirect:/books";
        model.addAttribute("book", book);
        return "book-form";
    }

    @PostMapping("/edit/{id}")
    public String editBook(@PathVariable Long id, @ModelAttribute Book book) {
        book.setId(id);
        bookRepository.save(book);
        return "redirect:/books";
    }

    @PostMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookRepository.deleteById(id);
        return "redirect:/books";
    }
} 