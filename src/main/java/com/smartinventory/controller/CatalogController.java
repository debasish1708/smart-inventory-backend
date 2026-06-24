package com.smartinventory.controller;

import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.CategoryResponse;
import com.smartinventory.dto.response.ProductSummaryResponse;
import com.smartinventory.entity.Category;
import com.smartinventory.entity.Product;
import com.smartinventory.repository.CategoryRepository;
import com.smartinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Public catalog endpoints — no authentication required.
 *
 * GET /api/catalog/categories           → all categories
 * GET /api/catalog/products             → all products
 * GET /api/catalog/products?categoryId= → products by category
 */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;

    // ── Categories ──────────────────────────────────────────────────────────

    /**
     * Returns all categories (id, name, description).
     * Frontend uses this to populate the category filter list.
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryRepository.findAll()
                .stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Categories fetched", categories));
    }

    // ── Products ─────────────────────────────────────────────────────────────

    /**
     * Returns products, optionally filtered by categoryId.
     *
     * @param categoryId optional — if absent all products are returned;
     *                   if present only products of that category are returned.
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getProducts(
            @RequestParam(required = false) Long categoryId) {

        List<Product> products = (categoryId != null)
                ? productRepository.findByCategoryId(categoryId)
                : productRepository.findAll();

        List<ProductSummaryResponse> response = products.stream()
                .map(this::toProductSummaryResponse)
                .collect(Collectors.toList());

        String message = (categoryId != null)
                ? "Products fetched for categoryId=" + categoryId
                : "All products fetched";

        return ResponseEntity.ok(ApiResponse.ok(message, response));
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private CategoryResponse toCategoryResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .build();
    }

    private ProductSummaryResponse toProductSummaryResponse(Product p) {
        return ProductSummaryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .sku(p.getSku())
                .brand(p.getBrand())
                .unit(p.getUnit())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .build();
    }
}
