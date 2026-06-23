package com.smartinventory.dto.response;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderResponse {
    private Long            id;
    private String          status;
    private LocalDateTime   orderDate;
    private LocalDateTime   deliveredDate;
    private String          supplierEmail;
    private String          retailerEmail;
    private List<OrderItemResponse> items;
    private java.math.BigDecimal totalAmount;
}
