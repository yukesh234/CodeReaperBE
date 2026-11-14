package com.codereaper.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class healthController {
    @GetMapping("/")
    public String sayHI(){
        return "hiiii";
    }
}
