package com.example.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    @Test
    void findAllReturnsStoredProducts() {
        when(products.findAll()).thenReturn(java.util.List.of(new Product("1", "Keyboard", 49.99)));

        assertEquals("Keyboard", productService.findAll().get(0).getName());
    }

    @Test
    void findByIdReturnsProduct() {
        when(products.findById("1")).thenReturn(java.util.Optional.of(new Product("1", "Keyboard", 49.99)));

        assertEquals(49.99, productService.findById("1").getPrice());
    }

    @Test
    void findByIdReturnsNotFoundForMissingProduct() {
        when(products.findById("missing")).thenReturn(java.util.Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.findById("missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateSavesChangedProduct() {
        Product existing = new Product("1", "Old name", 10.0);
        when(products.findById("1")).thenReturn(java.util.Optional.of(existing));
        when(products.save(existing)).thenReturn(existing);

        Product updated = productService.update("1", "New name", 20.0);

        assertEquals("New name", updated.getName());
        assertEquals(20.0, updated.getPrice());
        verify(products).save(existing);
    }

    @Test
    void updateDoesNotSaveMissingProduct() {
        when(products.findById("missing")).thenReturn(java.util.Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> productService.update("missing", "Name", 1.0));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(products, never()).save(any(Product.class));
    }

    @Test
    void deleteRemovesExistingProduct() {
        Product existing = new Product("1", "Keyboard", 49.99);
        when(products.findById("1")).thenReturn(java.util.Optional.of(existing));

        productService.delete("1");

        verify(products).delete(existing);
    }

    @Test
    void deleteReturnsNotFoundForMissingProduct() {
        when(products.findById("missing")).thenReturn(java.util.Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.delete("missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(products, never()).delete(any(Product.class));
    }
}