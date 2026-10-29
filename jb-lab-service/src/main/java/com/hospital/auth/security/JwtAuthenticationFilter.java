package com.hospital.auth.security;

import com.hospital.auth.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component // Enforces that Spring detects and manages this filter bean
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Ensure this matches your token utility class property definition exactly
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

                // Parse claims using your provider utility bean safely
                if (claims != null) {
                    String role = claims.get("role", String.class);

                    if (role != null) {
                        // Constructs the expected authority string (e.g., "ROLE_ADMIN")
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                        
                        var auth = new UsernamePasswordAuthenticationToken(
                                claims.getSubject(), 
                                null,
                                authorities
                        );
                        
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); // Prints the absolute stack trace to your application console
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
