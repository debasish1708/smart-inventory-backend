package com.smartinventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationResponse {
    private int categoriesCreated;
    private int productsCreated;
    private int retailersCreated;
    private int suppliersCreated;
    private int adminsCreated;
    private int retailerInventoryRecords;
    private int supplierInventoryRecords;
    private String defaultPassword;
    private String adminEmail;
}
