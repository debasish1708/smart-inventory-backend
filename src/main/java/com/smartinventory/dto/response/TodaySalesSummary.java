package com.smartinventory.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodaySalesSummary {
    private BigDecimal totalAmount;
    private long totalCount;
}
