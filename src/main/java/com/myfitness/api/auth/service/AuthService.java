package com.myfitness.api.auth.service;

import com.myfitness.api.auth.dto.LoginRequest;
import com.myfitness.api.auth.dto.LoginResponse;
import com.myfitness.api.auth.jwt.JwtProvider;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public AuthService(AuthenticationManager authenticationManager, JwtProvider jwtProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    public LoginResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(), req.password()));

        String token = jwtProvider.generate(auth);

        return new LoginResponse(token, "Bearer", jwtProvider.expiresInSeconds());
    }
}