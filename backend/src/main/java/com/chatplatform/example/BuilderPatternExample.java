package com.chatplatform.example;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;

import java.time.Instant;

/**
 * Example class demonstrating the Builder pattern usage
 * This showcases the benefits of using builders for complex object construction
 */
public class BuilderPatternExample {

    /**
     * Example: Creating a registration request with Builder pattern
     * Benefits: Readable, maintainable, less error-prone than positional parameters
     */
    public static RegisterRequest createRegistrationRequest() {
        return RegisterRequest.builder()
            .username("john_doe")
            .email("john.doe@company.com")
            .password("securePassword123")
            .displayName("John Doe")
            .build();
    }

    /**
     * Example: Creating a registration request with validation
     * Benefits: Ensures required fields are present before construction
     */
    public static RegisterRequest createValidatedRegistrationRequest() {
        return RegisterRequest.builder()
            .username("jane_smith")
            .email("jane.smith@company.com")
            .password("anotherSecurePassword456")
            .displayName("Jane Smith")
            .buildWithValidation(); // Throws exception if validation fails
    }

    /**
     * Example: Modifying an existing registration request
     * Benefits: Immutable objects with easy modification through builders
     */
    public static RegisterRequest modifyRegistrationRequest(RegisterRequest original) {
        return RegisterRequest.Builder.from(original)
            .email("newemail@company.com")
            .displayName("Updated Display Name")
            .build();
    }

    /**
     * Example: Creating a successful authentication response
     * Benefits: Clear, readable construction of complex objects
     */
    public static AuthResponse createSuccessAuthResponse(User user, String token) {
        return AuthResponse.builder()
            .token(token)
            .user(user)
            .successful()
            .build();
    }

    /**
     * Example: Creating a failed authentication response
     * Benefits: Fluent API makes intent clear
     */
    public static AuthResponse createFailedAuthResponse() {
        return AuthResponse.builder()
            .failed()
            .build();
    }

    /**
     * Example: Creating a custom authentication response
     * Benefits: Flexible construction with default values
     */
    public static AuthResponse createCustomAuthResponse(String token, User user, String tokenType) {
        return AuthResponse.builder()
            .token(token)
            .type(tokenType)
            .user(user)
            .issuedAt(Instant.now())
            .successful()
            .build();
    }

    /**
     * Example: Creating authentication response with validation
     * Benefits: Ensures business rules are enforced
     */
    public static AuthResponse createValidatedAuthResponse(String token, User user) {
        return AuthResponse.builder()
            .token(token)
            .user(user)
            .successful()
            .buildWithValidation(); // Throws exception if token or user is missing
    }

    /**
     * Example: Modifying an existing authentication response
     * Benefits: Easy modification of immutable objects
     */
    public static AuthResponse updateAuthResponseToken(AuthResponse original, String newToken) {
        return AuthResponse.Builder.from(original)
            .token(newToken)
            .issuedAt(Instant.now()) // Update timestamp
            .build();
    }

    /**
     * Comparison: Traditional constructor vs Builder pattern
     */
    public static void demonstrateReadabilityComparison() {
        // Traditional approach - hard to read, error-prone
        // RegisterRequest traditional = new RegisterRequest("user", "email@test.com", "password", "Display Name");
        
        // Builder pattern - readable, maintainable
        RegisterRequest builder = RegisterRequest.builder()
            .username("user")
            .email("email@test.com")
            .password("password")
            .displayName("Display Name")
            .build();
        
        // The builder pattern is especially beneficial when:
        // 1. Parameters might be confused (order matters in constructors)
        // 2. Optional parameters exist
        // 3. You want to enforce validation
        // 4. You need to modify immutable objects
        // 5. You want to provide default values
    }

    /**
     * Example: Partial object construction with defaults
     * Benefits: Only set what you need, rely on sensible defaults
     */
    public static AuthResponse createMinimalAuthResponse(String token, User user) {
        return AuthResponse.builder()
            .token(token)
            .user(user)
            // type defaults to "Bearer"
            // issuedAt defaults to Instant.now()
            // success defaults to true
            .build();
    }
}