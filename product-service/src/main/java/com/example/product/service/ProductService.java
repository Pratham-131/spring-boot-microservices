package com.example.product.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;

@Service
public class ProductService {
    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public List<Product> findAll() {
        return products.findAll();
    }

    public Product create(String name, double price) {
        return products.save(new Product(null, name, price));
    }

    public Product findById(String id) {
        return products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Product update(String id, String name, double price) {
        Product product = findById(id);
        product.setName(name);
        product.setPrice(price);
        return products.save(product);
    }

    public void delete(String id) {
        Product product = findById(id);
        products.delete(product);
    }
}