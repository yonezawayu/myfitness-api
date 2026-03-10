package com.myfitness.api.user.controller;

import com.myfitness.api.user.dto.UserCreateRequest;
import com.myfitness.api.user.dto.UserResponse;
import com.myfitness.api.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest req) {
        return userService.create(req);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email);
    }
}