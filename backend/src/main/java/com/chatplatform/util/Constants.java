package com.chatplatform.util;

public final class Constants {

    private Constants() {
        // private constructor to prevent instantiation
    }

    public static final String ERROR = "error";
    public static final String ADMIN_ACCESS_REQUIRED = "Admin access required";
    public static final String USER_ID = "userId";

    // AuthController constants
    public static final String LOGIN_SUCCESSFUL = "Login successful";
    public static final String LOGIN_FAILED_SERVER_ERROR = "Login failed due to server error";
    public static final String USER_REGISTERED_SUCCESSFULLY = "User registered successfully";
    public static final String REGISTRATION_FAILED_SERVER_ERROR = "Registration failed due to server error";
    public static final String USER_RETRIEVED_SUCCESSFULLY = "User retrieved successfully";
    public static final String INVALID_OR_EXPIRED_TOKEN = "Invalid or expired token";
    public static final String FAILED_TO_RETRIEVE_USER_INFO = "Failed to retrieve user information";
    public static final String LOGOUT_SUCCESSFUL = "Logout successful";
    public static final String LOGOUT_FAILED = "Logout failed";
    public static final String PASSWORD_RESET_EMAIL_SENT = "Password reset email sent if account exists";
    public static final String PASSWORD_RESET_SUCCESSFUL = "Password reset successful";
    public static final String INVALID_OR_EXPIRED_RESET_TOKEN = "Invalid or expired reset token";
    public static final String PASSWORD_CHANGED_SUCCESSFULLY = "Password changed successfully";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred. Please try again later.";
    public static final String CURRENT_PASSWORD_REQUIRED = "Current password is required";
    public static final String NEW_PASSWORD_REQUIRED = "New password is required";
    public static final String NEW_PASSWORD_MUST_BE_DIFFERENT = "New password must be different from current password";
    public static final String PASSWORD_MIN_LENGTH = "Password must be at least 8 characters long";
    public static final String PASSWORD_NO_CONSECUTIVE_CHARS = "Password cannot contain more than 2 consecutive identical characters";
    public static final String PASSWORD_TOO_COMMON = "Password contains commonly used patterns. Please choose a more unique password.";
    public static final String PASSWORD_REQUIREMENTS = "Password must contain at least 3 of the following: ";
    public static final String LOWERCASE_LETTERS = "lowercase letters, ";
    public static final String UPPERCASE_LETTERS = "uppercase letters, ";
    public static final String NUMBERS = "numbers, ";
    public static final String SPECIAL_CHARACTERS = "special characters (!@#$%^&*), ";
    public static final String PROFILE_UPDATED_SUCCESSFULLY = "Profile updated successfully";
    public static final String NO_CHANGES_TO_UPDATE = "No changes to update";
    public static final String PROFILE_UPDATE_FAILED = "Profile update failed";
    public static final String USER_NOT_FOUND = "User not found";

    // HealthController constants
    public static final String STATUS = "status";
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
    public static final String RESPONSE_TIME = "responseTime";
    public static final String DETAILS = "details";

    // MessageController constants
    public static final String AUTHENTICATION_REQUIRED = "Authentication required";
    public static final String FAILED_TO_FETCH_MESSAGES = "Failed to fetch messages: ";

    // Common API response keys
    public static final String SUCCESS = "success";
    public static final String MESSAGE = "message";
    public static final String DATA = "data";

    // Common WebSocket message keys
    public static final String TYPE = "type";
    public static final String ACK = "ack";
    public static final String PING = "ping";

    // Common error messages
    public static final String USERNAME_ALREADY_EXISTS = "Username already exists";
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    public static final String USER_NOT_FOUND_WITH_ID = "User not found with ID: ";
    public static final String CONVERSATION_NOT_FOUND = "Conversation not found: ";
    public static final String PARTICIPANT_NOT_FOUND = "Participant not found: ";
    public static final String CREATOR_NOT_FOUND = "Creator not found: ";
    public static final String CANNOT_UPDATE_SETTINGS_FOR_NON_GROUP = "Cannot update settings for non-group conversation";
    public static final String CANNOT_ADD_USERS_TO_DIRECT_CONVERSATIONS = "Cannot add users to direct conversations";
    public static final String ONLY_GROUP_OWNERS_CAN_DELETE_GROUPS = "Only group owners can delete groups";
    public static final String NO_ACCESS_TO_CONVERSATION = "You do not have access to this conversation";

    // HealthController constants
    public static final String BACKEND_IS_WORKING_CORRECTLY = "Backend is working correctly";

}
