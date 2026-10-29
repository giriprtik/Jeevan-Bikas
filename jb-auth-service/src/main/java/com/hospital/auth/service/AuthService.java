package com.hospital.auth.service;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.LoginResponse;
import com.hospital.auth.dto.UserRegisterRequest;
import com.hospital.auth.model.UserAccount;
import com.hospital.auth.repository.UserAccountRepository;
import com.hospital.auth.util.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuthService { // Actively driving Entity Control

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider; // REPAIRED: Injected missing cryptographic component

    // Explicit constructor injecting all dependencies required by the business logic
    public AuthService(UserAccountRepository userAccountRepository, 
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public UserAccount registerNewUser(UserRegisterRequest request) {
        // 1. Business Logic Control Rule: Validate if entity already exists
        if (userAccountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered in the hospital system.");
        }
        
        if (userAccountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken in the hospital system.");
        }

        // 2. Control flow handling data transformation
        UserAccount newUser = new UserAccount();
        newUser.setUsername(request.getUsername().trim());
        newUser.setEmail(request.getEmail().trim());
        
        // Enforce hospital security rule: encrypt passwords before saving
        newUser.setPassword(passwordEncoder.encode(request.getPassword())); 
        
        // Handle role parsing rules safely
        String finalRole = request.getRole();
        newUser.setRole((finalRole == null || finalRole.trim().isEmpty()) ? "PATIENT" : finalRole.trim().toUpperCase());
        newUser.setEnabled(true);
        newUser.setCreatedAt(OffsetDateTime.now());

        // 3. Command the repository to save the structural Entity state
        return userAccountRepository.save(newUser);
    }

    public LoginResponse authenticateUser(LoginRequest request) {
        // REPAIRED: Changed 'userRepository' to 'userAccountRepository' to match constructor variable
        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password."));
        
        System.out.println("Encrypted version of what the user typed: [" + passwordEncoder.encode(request.getPassword()) + "]");

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password.");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("This user account has been deactivated.");
        }

        // Issue cryptographic access tokens based on internal Entity properties
        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
        
        // REPAIRED: Structured response mapping to cleanly match the LoginResponse template
        return new LoginResponse(
                token, 
                "Bearer", 
                tokenProvider.getExpirationMs(), 
                user.getUsername(), 
                user.getRole()
        );
    }
}
