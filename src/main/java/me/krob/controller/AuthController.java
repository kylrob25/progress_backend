package me.krob.controller;

import me.krob.model.Role;
import me.krob.model.User;
import me.krob.model.auth.*;
import me.krob.model.token.RefreshToken;
import me.krob.repository.UserRepository;
import me.krob.security.service.UserDetailsImpl;
import me.krob.service.RefreshTokenService;
import me.krob.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.logging.Logger;

@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getUsername() == null ||
                loginRequest.getPassword() == null) {
            return ResponseEntity.badRequest().body(new AuthResponse("One or more required field is missing."));
        }

        if (loginRequest.getUsername().isEmpty() ||
                loginRequest.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse("One or more required field is empty."));
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtUtils.generate(authentication);
        RefreshToken refreshToken = refreshTokenService.create(user.getUsername());

        return ResponseEntity.ok()
                .body(new LoginResponse(
                        token,
                        refreshToken.getToken(),
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRoles()
                ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        if (registerRequest.getUsername() == null ||
                registerRequest.getSurname() == null ||
                registerRequest.getForename() == null ||
                registerRequest.getEmail() == null ||
                registerRequest.getPassword() == null) {
            return ResponseEntity.badRequest().body(new AuthResponse("One or more required field is missing."));
        }

        if (registerRequest.getUsername().isEmpty() ||
                registerRequest.getSurname().isEmpty()||
                registerRequest.getForename().isEmpty() ||
                registerRequest.getEmail().isEmpty() ||
                registerRequest.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse("One or more required field is empty."));
        }

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new AuthResponse("An account with that username already exists."));
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new AuthResponse("An account with that email already exists."));
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setForename(registerRequest.getForename());
        user.setSurname(registerRequest.getSurname());
        user.setEmail(registerRequest.getEmail());
        user.setRoles(Set.of(Role.USER));
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        userRepository.save(user);

        return ResponseEntity.ok().body(new AuthResponse("Successfully registered the new user."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody TokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Empty refresh token."));
        }

        Logger.getGlobal().info(refreshToken);

        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verify)
                .map(RefreshToken::getUsername)
                .map(username -> {
                    String token = jwtUtils.generate(username);
                    Logger.getGlobal().info(String.format("Generated new token: %s", token));
                    return ResponseEntity.ok()
                            .body(new TokenResponse(token, refreshToken));
                })
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            try {
                Object principal = authentication.getPrincipal();
                Logger.getGlobal().info(principal.getClass().getSimpleName());

                if (principal instanceof UserDetailsImpl user) {
                    Logger.getGlobal().info(String.format("Deleting refresh token from service for %s", user.getUsername()));
                    refreshTokenService.deleteByUsername(user.getId());
                } else {
                    Logger.getGlobal().info(principal.toString());
                }
            } catch (Throwable throwable) {
                Logger.getGlobal().info(throwable.getMessage());
            }
        }

        return ResponseEntity.ok()
                .body(new AuthResponse("Success."));
    }
}
