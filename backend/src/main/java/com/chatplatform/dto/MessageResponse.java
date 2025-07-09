package com.chatplatform.dto;

/**
 * Standardized response DTO for API messages and errors
 * @param success Indicates if the request was successful
 * @param message Response message or error description
 * @param data Optional data payload
 * @param <T> Type of the data payload
 */
public record MessageResponse<T>(
    boolean success,
    String message,
    T data
) {
    public static <T> MessageResponse<T> success(String message, T data) {
        return new MessageResponse<>(true, message, data);
    }

    public static <T> MessageResponse<T> success(T data) {
        return success("Operation successful", data);
    }

    public static MessageResponse<Void> success(String message) {
        return success(message, null);
    }

    public static <T> MessageResponse<T> error(String message, T data) {
        return new MessageResponse<>(false, message, data);
    }

    public static MessageResponse<Void> error(String message) {
        return error(message, null);
    }
}
