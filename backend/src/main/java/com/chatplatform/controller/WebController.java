package com.chatplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to serve the React frontend from Spring Boot
 * This enables single-service deployment by serving both API and frontend from one app
 */
@Controller
public class WebController {

    /**
     * Serve the React app's index.html for all non-API routes
     * This allows React Router to handle client-side routing
     */
    @GetMapping(value = {
            "/", 
            "/login", 
            "/chat/**", 
            "/conversations/**",
            "/settings/**"
    })
    public String serveReactApp() {
        return "forward:/index.html";
    }
    
    /**
     * Explicit mapping for the root path
     */
    @RequestMapping("/")
    public String index() {
        return "forward:/index.html";
    }
    
    /**
     * Health check endpoint for the combined app
     */
    @GetMapping("/health")
    public String health() {
        return "forward:/api/health/status";
    }
}