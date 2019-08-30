package com.microfocus.profiling.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleRestController {

    @RequestMapping(value = "/hello")
    public String helloSpring() {
        return "Hello, Spring Boot!";
    }
}