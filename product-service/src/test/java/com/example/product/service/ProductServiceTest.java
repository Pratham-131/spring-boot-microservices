package com.example.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository products;
    @InjectMocks ProductService productService;

    @Test
    void createPersistsProduct() {
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.create("Keyboard", 49.99);

        assertEquals("Keyboard", created.getName());
        assertEquals(49.99, created.getPrice());
        verify(products).save(any(Product.class));
    }
}