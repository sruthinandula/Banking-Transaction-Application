package com.example.bank.auth;

import com.example.bank.auth.AuthDtos.LoginRequest;
import com.example.bank.auth.AuthDtos.RegisterRequest;
import com.example.bank.auth.AuthDtos.TokenResponse;
import com.example.bank.auth.AuthDtos.UserResponse;
import com.example.bank.exception.BankingExceptions.DuplicateResourceException;
import com.example.bank.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken");
        }
        AppUser saved = userRepository.save(
            new AppUser(request.username(), passwordEncoder.encode(request.password())));
        return new UserResponse(saved.getId(), saved.getUsername());
    }

    public TokenResponse login(LoginRequest request) {
        // Throws BadCredentialsException -> 401 via GlobalExceptionHandler
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return new TokenResponse(jwtService.generateToken(request.username()), "Bearer",
            jwtService.getExpirationSeconds());
    }
}
