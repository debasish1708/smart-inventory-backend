package com.smartinventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminAnalyticsResponse {
    private long totalUsers;
    private long activeUsers;
    private long pendingUsers;
    private long blockedUsers;
    
    private long subscriptionCount;
    private BigDecimal subscriptionRevenue;
    
    private long retailerSalesCount;
    private BigDecimal retailerSalesRevenue;
    
    private long supplierSalesCount;
    private BigDecimal supplierSalesRevenue;
    
    private List<MonthlyRevenue> monthlyRetailerRevenue;
    private List<MonthlyRevenue> monthlySupplierRevenue;
    
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class MonthlyRevenue {
        private String month;
        private BigDecimal revenue;
    }
}
