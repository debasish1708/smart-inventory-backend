package com.smartinventory.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetailerDashboardResponse {
    private long totalInventoryItems;
    private long totalInventoryQuantity;
    private BigDecimal totalInventoryValue;
    private long lowStockCount;
    
    private BigDecimal totalSalesRevenue;
    private long totalSalesTransactions;
    private BigDecimal todaySalesRevenue;
    private long todaySalesTransactions;
    
    private BigDecimal totalPurchases;
    private long totalSupplierOrders;
    private long pendingSupplierOrders;
    
    private String topSupplierName;
    private String topProductSold;
}
