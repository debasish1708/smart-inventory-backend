package com.smartinventory.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {

    private Long   id;
    private String name;
    private String description;
}
