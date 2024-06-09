package com.baeldung.springsociallogin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Hello, public user!";
    }

    @GetMapping("/secure")
    public String secured() {
        return "Hello, logged in user!";
    }
}
