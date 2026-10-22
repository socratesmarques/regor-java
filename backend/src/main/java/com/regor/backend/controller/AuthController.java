package com.regor.backend.controller;
import com.regor.backend.dto.Api.*;
import com.regor.backend.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service=service; }
    @PostMapping("/auth/register") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Session register(@Valid @RequestBody Register request) { return service.register(request); }
    @PostMapping("/auth/login")
    public Session login(@Valid @RequestBody Login request) { return service.login(request); }
    @GetMapping("/users/me")
    public Account me(@AuthenticationPrincipal Jwt jwt) { return service.account(UUID.fromString(jwt.getSubject())); }
    @PutMapping("/users/me/password") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void password(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody PasswordChange request) {
        service.changePassword(UUID.fromString(jwt.getSubject()),request);
    }
}
