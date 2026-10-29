package com.hospital.auth.config;

import com.hospital.auth.security.JwtAuthenticationFilter;
import com.hospital.auth.security.CustomAuthenticationEntryPoint;
import com.hospital.auth.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final UserAccountRepository userAccountRepository; // Injected to back the UserDetailsService mapping

    // Explicit constructor injection avoiding Lombok overhead compilation issues
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, 
                          CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                          UserAccountRepository userAccountRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.userAccountRepository = userAccountRepository;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Bridges Spring Security's load requirements directly to your database Entity
        return username -> userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User identity not found in database: " + username));
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        
        // Return a fully configured ProviderManager back to the application framework container
        return new ProviderManager(List.of(authProvider));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disables CSRF protection for stateless API design
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // FIX: Strictly limit public access to authentication endpoints only
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Secure administrative and standard user management zones
                .requestMatchers("/api/v1/auth/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/auth/management/**").hasAnyRole("ADMIN", "MANAGER")
                
                // Enforce authentication context constraints across all other routes
                .anyRequest().authenticated()
            )
            // Binds the custom exception mapper entry point for client handling
            .exceptionHandling(exception -> exception.authenticationEntryPoint(customAuthenticationEntryPoint))
            // Places our local entity verification filter ahead of standard credential filtering
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
