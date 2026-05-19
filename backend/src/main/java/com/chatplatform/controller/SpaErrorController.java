package com.chatplatform.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom error controller for SPA routing.
 * Forwards SPA route 404s to index.html (React Router handles them).
 * Returns 404 for missing static assets so browsers don't parse HTML as JS.
 * Returns JSON for API errors.
 */
@Controller
public class SpaErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        int status = statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR.value();

        if (requestUri == null) {
            requestUri = "";
        }

        // API routes → JSON error response
        if (requestUri.startsWith("/api/")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", "Resource not found");
            return ResponseEntity.status(status).body(body);
        }

        // Static asset 404s (JS/CSS/images) → return 404, not index.html.
        // Forwarding index.html here causes browsers to parse HTML as JS.
        if (requestUri.startsWith("/static/") || requestUri.contains(".")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");
        }

        // SPA routes (e.g. /chat, /login, /profile) → forward to index.html
        return "forward:/index.html";
    }
}
