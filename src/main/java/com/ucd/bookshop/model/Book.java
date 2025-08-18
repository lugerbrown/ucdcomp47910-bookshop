package com.ucd.bookshop.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.CascadeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name= "books")
public class Book {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    @Size(max = 150)
    private String bookName;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "book_author",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<Author> authors = new HashSet<>();

    @NotBlank
    @Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$", message = "Invalid ISBN (expect ISBN-10 or ISBN-13 numeric)")
    private String isbn;

    @Min(value = 1450, message = "Year too early")
    @Max(value = 2100, message = "Year too large")
    private Integer year;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be >= 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have max 8 digits and 2 decimals")
    private Double price;

    @Min(0)
    @Max(100000)
    private Integer numberOfCopies;

    public Book() {
        super();
    }
    public Book(Long id, String bookName, String isbn, Integer year, Double price, Integer numberOfCopies) {
        this.id = id;
        this.bookName = bookName;
        this.isbn = isbn;
        this.year = year;
        this.price = price;
        this.numberOfCopies = numberOfCopies;
    }

    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }

    public Set<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Author> authors) {
        this.authors = authors;
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

