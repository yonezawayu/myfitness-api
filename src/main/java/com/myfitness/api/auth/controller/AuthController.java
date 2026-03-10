package com.myfitness.api.auth.controller;

import com.myfitness.api.auth.dto.LoginRequest;
import com.myfitness.api.auth.dto.LoginResponse;
import com.myfitness.api.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}