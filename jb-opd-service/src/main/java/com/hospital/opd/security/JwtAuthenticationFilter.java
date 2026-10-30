package com.hospital.opd.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.hospital.opd.util.JwtTokenProvider; 
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String tokenString = header.substring(7);
                Claims claims = jwtTokenProvider.parse(tokenString);

                // 1. FIXED: Look for the correct plural "roles" key matching your Auth service claim
                if (claims != null && claims.get("roles") != null) {
                    String rolesString = claims.get("roles", String.class); // e.g. "NURSE" or "NURSE,STAFF"
                    
                    // 2. FIXED: Split the comma-separated string cleanly into a Spring authorities collection
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                            .map(String::trim)
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    System.out.println("=== [JWT SECURITY CHECK SUCCESS] ===");
                    System.out.println("User logging in: " + claims.getSubject());
                    System.out.println("Raw roles from JWT token: " + rolesString);
                    System.out.println("Granted Authorities set in Spring: " + authorities);
                    System.out.println("====================================");
                    
                    var auth = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), 
                            null,
                            authorities
                    );
                    
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                logger.error("JWT Authentication signature validation failed: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
