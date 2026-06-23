package com.smartinventory.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionResponse {
    private Long          id;
    private String        planName;
    private String        status;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
}
