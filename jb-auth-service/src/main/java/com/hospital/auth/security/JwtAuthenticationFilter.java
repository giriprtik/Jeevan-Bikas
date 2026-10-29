package com.hospital.auth.security;

import com.hospital.auth.model.UserAccount;
import com.hospital.auth.repository.UserAccountRepository;
import com.hospital.auth.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userAccountRepository; // Added database access

    // Constructor injection for both beans
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserAccountRepository userAccountRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String tokenString = header.substring(7);
                Claims claims = jwtTokenProvider.parse(tokenString);

                if (claims != null && claims.getSubject() != null) {
                    String username = claims.getSubject();

                    // FIX: Fetch the actual Entity state from the database to check current status
                    UserAccount userAccount = userAccountRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("User no longer exists in system."));

                    // Enforce live security control check
                    if (userAccount.isEnabled() && SecurityContextHolder.getContext().getAuthentication() == null) {
                        
                        // FIX: Pass the actual 'userAccount' object instead of a raw string subject
                        var auth = new UsernamePasswordAuthenticationToken(
                                userAccount, 
                                null,
                                userAccount.getAuthorities() // Uses entity's direct authorities mapping
                        );
                        
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception e) {
                this.logger.error("Auth Service JWT Validation Error: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
