package com.smartinventory.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponse {
    private Long id;
    private LocalDateTime saleDate;
    private BigDecimal totalAmount;
    private String retailerEmail;
    private List<SaleItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        private String unit;
    }
}
