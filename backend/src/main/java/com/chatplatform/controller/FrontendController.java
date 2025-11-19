package com.chatplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {
    
    /**
     * Forward all non-API routes to index.html for React Router to handle
     */
    @GetMapping(value = {"/", "/login", "/chat/**", "/reset-password", "/conversations/**", "/settings/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
