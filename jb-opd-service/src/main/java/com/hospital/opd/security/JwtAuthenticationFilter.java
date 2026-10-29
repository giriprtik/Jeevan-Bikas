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
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // Reads the shared key settings directly out of application.properties
    // 2. INJECT the dependency using an explicit constructor
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

                // Combines assertion boundaries to reduce indentation nesting
                if (claims != null && claims.get("role") != null) {
                    String role = claims.get("role", String.class);
                    
                    // Formats target security context strings clean
                    String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    var authorities = List.of(new SimpleGrantedAuthority(formattedRole));
                    
                    var auth = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), 
                            null,
                            authorities
                    );
                    
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // Production log practice: avoids long printStackTrace console locking blocks
                logger.error("JWT Authentication signature validation failed: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
