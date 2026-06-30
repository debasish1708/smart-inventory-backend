package com.smartinventory.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetailerAnalyticsResponse {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private String topProduct;
    private double monthlyGrowth;
    
    private List<MonthlySalesTrend> monthlySalesTrend;
    private List<RegionalDemandTrend> regionalDemand;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlySalesTrend {
        private String month;
        private BigDecimal sales;
        private double growth;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegionalDemandTrend {
        private String productName;
        private double demandScore;
        private String trend;
        private BigDecimal avgPrice;
    }
}
