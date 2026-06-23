package com.smartinventory.dto.response;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemResponse {
    private Long       id;
    private String     productName;
    private Integer    quantity;
    private BigDecimal price;
    private String     unit;
}
