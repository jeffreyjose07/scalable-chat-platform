package com.chatplatform.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * Custom error controller for SPA routing
 * Returns index.html for 404 errors on non-API routes, allowing React Router to handle them
 * Returns JSON error for API routes
 */
@Controller
public class SpaErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");

        // For API routes, return JSON error
        if (requestUri != null && requestUri.startsWith("/api/")) {
            return ResponseEntity
                    .status(statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(Map.of(
                            "success", false,
                            "message", "Resource not found",
                            "data", null
                    ));
        }

        // For non-API routes (frontend routes), forward to index.html for React Router
        // This handles page refresh on SPA routes like /chat, /login, etc.
        if (statusCode != null && statusCode == HttpStatus.NOT_FOUND.value()) {
            return "forward:/index.html";
        }

        // For other errors on non-API routes, also forward to SPA
        // Let the frontend handle displaying appropriate error UI
        return "forward:/index.html";
    }
}
