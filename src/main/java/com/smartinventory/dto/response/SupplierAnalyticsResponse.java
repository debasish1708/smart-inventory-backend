package com.smartinventory.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierAnalyticsResponse {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private String topProduct;
    private double avgRating;
    
    private List<MonthlySalesTrend> monthlySalesTrend;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlySalesTrend {
        private String month;
        private BigDecimal sales;
        private double growth;
    }
}
