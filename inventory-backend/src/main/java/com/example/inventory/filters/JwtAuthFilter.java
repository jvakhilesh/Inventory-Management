
package com.example.inventory.filters;

import java.io.IOException;
import java.util.List;

import com.example.inventory.dto.AuthUserDTO;
import com.example.inventory.service.AuthClientService;
import com.example.inventory.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthClientService authClientService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        log.info("LOG-1: JwtAuthFilter entered for {} {}",
                request.getMethod(), request.getRequestURI());

        String token = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        boolean tokenPresent = token != null && !token.isBlank();
        log.info("LOG-2: Access token present: {}", tokenPresent);

        if (!tokenPresent) {
            log.info("LOG-3: No access token; continuing filter chain");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            log.info("LOG-4: Validating access token");

            if (!jwtService.isAccessToken(token)) {
                log.warn("LOG-5: Access token is invalid");

                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token");
                return;
            }

            log.info("LOG-6: Calling Auth Service /auth/me");

            AuthUserDTO user = authClientService.getCurrentUser(token);

            log.info("LOG-7: Auth Service call succeeded; user is null: {}",
                    user == null);

            if (user == null
                    || user.id() == null
                    || user.role() == null
                    || !user.role().matches("ADMIN|EMPLOYEE|USER")) {

                log.warn("LOG-8: Auth Service returned invalid user details");

                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid user or role");
                return;
            }

            if (!user.id().equals(jwtService.getUserId(token))) {
                log.warn("LOG-9: User ID does not match token");

                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "User mismatch");
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.id(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + user.role()
                                    )
                            )
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            log.info("LOG-10: Authentication set for user ID {} with role {}",
                    user.id(), user.role());

        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();

            log.error("LOG-11: JWT validation or token processing failed", exception);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Invalid or expired access token\"}"
            );
            response.getWriter().flush();
            return;
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized exception) {
            SecurityContextHolder.clearContext();
            log.warn("LOG-11b: Auth Service returned 401 Unauthorized for token");
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Access token expired or unauthorized");
            return;
        } catch (RestClientException exception) {
            SecurityContextHolder.clearContext();

            log.error("LOG-12: Auth Service request failed", exception);

            log.info("LOG-14: Sending HTTP 503 because Auth Service is unavailable");

            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":503,\"error\":\"Authentication service is unavailable\"}"
            );
            response.getWriter().flush();
            log.info("LOG-15: Response status after sendError: {}",
                    response.getStatus());
            return;
        }

        log.info("LOG-13: Continuing Spring Security filter chain");

        filterChain.doFilter(request, response);
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + message + "\"}");
        response.getWriter().flush();
    }
}
