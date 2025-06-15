package com.ucd.bookshop.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name= "books")
public class Book {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    private String book_name;

    @NotBlank
    private String author_name;

    @NotBlank
    private String isbn;

    private Integer year;
    private Double price;
    private Integer numberOfCopies;

    public Book() {
        super();
    }
    public Book(Long id, String book_name, String author_name, String isbn, Integer year, Double price, Integer numberOfCopies) {
        this.id = id;
        this.book_name = book_name;
        this.author_name = author_name;
        this.isbn = isbn;
        this.year = year;
        this.price = price;
        this.numberOfCopies = numberOfCopies;
    }

    public String getBook_name() {
        return book_name;
    }

    public void setBook_name(String book_name) {
        this.book_name = book_name;
    }

    public String getAuthor_name() {
        return author_name;
    }
    public void setAuthor_name(String author_name) {
        this.author_name = author_name;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getNumberOfCopies() { return numberOfCopies; }
    public void setNumberOfCopies(Integer numberOfCopies) { this.numberOfCopies = numberOfCopies; }

}

