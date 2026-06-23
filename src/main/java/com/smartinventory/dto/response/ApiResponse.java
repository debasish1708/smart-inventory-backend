package com.smartinventory.dto.response;
import lombok.*;
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;
    public static <T> ApiResponse<T> ok(String msg, T data) { return new ApiResponse<>(true, msg, data); }
    public static <T> ApiResponse<T> ok(String msg) { return new ApiResponse<>(true, msg, null); }
    public static <T> ApiResponse<T> fail(String msg) { return new ApiResponse<>(false, msg, null); }
}
