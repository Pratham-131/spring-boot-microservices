package com.example.product.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.product.config.SecurityConfig;
import com.example.product.model.Product;
import com.example.product.service.ProductService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {
        private static final String SECRET = Base64.getEncoder()
                    .encodeToString(Jwts.SIG.HS256.key().build().getEncoded());
    @Autowired MockMvc mockMvc;
    @MockBean ProductService productService;

        @DynamicPropertySource
        static void jwtSecret(DynamicPropertyRegistry registry) {
                registry.add("jwt.secret", () -> SECRET);
        }

    @Test
    void createWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Keyboard\",\"price\":49.99}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listIsPublic() throws Exception {
        when(productService.findAll()).thenReturn(List.of(new Product("1", "Keyboard", 49.99)));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Keyboard"));
    }

    @Test
    void getByIdIsPublic() throws Exception {
        when(productService.findById("1")).thenReturn(new Product("1", "Keyboard", 49.99));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void getMissingProductReturnsStandardNotFound() throws Exception {
        when(productService.findById("missing"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        mockMvc.perform(get("/products/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void updateWithoutTokenReturnsStandardUnauthorized() throws Exception {
        mockMvc.perform(put("/products/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Keyboard\",\"price\":49.99}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deleteWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPriceReturnsValidationError() throws Exception {
        mockMvc.perform(post("/products").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Keyboard\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void validTokenCanCreateProduct() throws Exception {
        when(productService.create("Keyboard", 49.99)).thenReturn(new Product("1", "Keyboard", 49.99));

        mockMvc.perform(post("/products").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Keyboard\",\"price\":49.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void validTokenCanUpdateProduct() throws Exception {
        when(productService.update(eq("1"), eq("Keyboard"), eq(49.99)))
                .thenReturn(new Product("1", "Keyboard", 49.99));

        mockMvc.perform(put("/products/1").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Keyboard\",\"price\":49.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void validTokenCanDeleteProduct() throws Exception {
        mockMvc.perform(delete("/products/1").header("Authorization", bearerToken()))
                .andExpect(status().isNoContent());

        verify(productService).delete("1");
    }

    private String bearerToken() {
        return "Bearer " + Jwts.builder().subject("user@example.com")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}