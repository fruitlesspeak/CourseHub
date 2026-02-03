package com.example.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*")  // Allow requests from frontend
public class HelloController {

    @GetMapping("/api/hello")
    public String hello() {
        return "Hello from Spring Boot!";
    }
}
