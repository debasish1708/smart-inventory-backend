package com.smartinventory.dto.response;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalyticsResponse {
    private long       totalOrders;
    private BigDecimal totalRevenue;
    private String     topProduct;
    private double     monthlyGrowth;
}
