package com.smartinventory.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class SaleRequest {
    private List<SaleItemRequest> items;

    @Data
    public static class SaleItemRequest {
        private Long productId;
        private Integer quantity;
    }
}
