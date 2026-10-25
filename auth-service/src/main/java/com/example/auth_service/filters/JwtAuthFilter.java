package com.example.auth_service.filters;

import java.io.IOException;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.example.auth_service.entity.User;
import com.example.auth_service.service.JwtService;
import com.example.auth_service.service.UserServiceImp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserServiceImp userServiceImp;
    private final HandlerExceptionResolver handlerExceptionResolver;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/auth/login")
                || path.equals("/auth/signup")
                || path.equals("/auth/verify-otp")
                || path.equals("/auth/resend-otp")
                || path.equals("/auth/refresh")
                || path.equals("/auth/csrf")
                || path.equals("/auth/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain filterChain)throws ServletException, IOException {
        try {
            Cookie[] cookies = request.getCookies();
            String token = null;
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
            // No access token → continue the request
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }
            // Make sure the token is actually an ACCESS token
            if (!jwtService.isAccessToken(token)) {
                throw new BadCredentialsException("Invalid access token");
            }
            // Get user ID from JWT
            final Long id = jwtService.getUserId(token);
            if (id != null) {
                User user = userServiceImp.getUserById(id);
                // Reject access tokens belonging to inactive accounts.
                if (!user.isEnabled()) {
                    throw new BadCredentialsException(
                            "Account is not active"
                    );
                }
                // Validate token against the user
                if (!jwtService.validateToken(token, user)) {
                    throw new BadCredentialsException("Invalid Token");
                }
                // Create authentication object
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        );
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );
                // Store authentication in SecurityContext
                SecurityContextHolder.getContext()
                        .setAuthentication(authenticationToken);
            }
            // Continue request
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"Access token expired. Please login again.\"}"
            );
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(
                    request, response, null, e
            );
        }

    }
}
