package com.example.book.service;

import com.example.book.common.ResourceNotFoundException;
import com.example.book.dto.BookCreateRequest;
import com.example.book.dto.BookUpdateRequest;
import com.example.book.entity.Book;
import com.example.book.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookMapper bookMapper;

    public List<Book> findAll() {
        return bookMapper.findAll();
    }

    public Book findById(Long id) {
        Book book = bookMapper.findById(id);
        if (book == null) {
            throw new ResourceNotFoundException("图书不存在，ID: " + id);
        }
        return book;
    }

    public void create(BookCreateRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPrice(request.getPrice());
        book.setPublishDate(request.getPublishDate());
        book.setDescription(request.getDescription());
        bookMapper.insert(book);
    }

    public void update(BookUpdateRequest request) {
        findById(request.getId());
        Book book = new Book();
        book.setId(request.getId());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPrice(request.getPrice());
        book.setPublishDate(request.getPublishDate());
        book.setDescription(request.getDescription());
        bookMapper.update(book);
    }

    public void deleteById(Long id) {
        findById(id);
        bookMapper.deleteById(id);
    }
}
