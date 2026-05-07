package com.example.gestioncollecteinfo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/*
 * Stateless JWT utility — replaces HttpSession for authentication.
 *
 * How it works:
 *   1. At login, AuthServlet calls generateToken(userId, role)
 *   2. The token is returned in the JSON response
 *   3. The client stores it and sends it as: Authorization: Bearer <token>
 *   4. AuthFilter calls parseToken() on every request to validate it
 *
 * Secret key:
 *   - Read from the JWT_SECRET environment variable (required in production)
 *   - Falls back to a default dev key for local development
 *   - Must be at least 32 characters (256 bits) for HS256
 *
 * Token lifetime: 24 hours
 */
public class JwtUtil {

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    /*
     * Default key used ONLY for local development.
     * In production (Railway), set the JWT_SECRET environment variable.
     * Must be at least 32 chars for HMAC-SHA256.
     */
    private static final String DEFAULT_SECRET =
            "dev-only-secret-key-change-me-in-production-32chars!";

    private static final SecretKey SECRET_KEY = resolveKey();

    private static SecretKey resolveKey() {
        String env = System.getenv("JWT_SECRET");
        String secret = (env != null && !env.isBlank()) ? env : DEFAULT_SECRET;
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /*
     * Generate a signed JWT containing the user's ID and role.
     *
     * Claims:
     *   sub  = userId (as string)
     *   role = "ADMIN" or "EMPLOYEE"
     *   iat  = issued-at timestamp
     *   exp  = expiration timestamp (24h from now)
     */
    public static String generateToken(Long userId, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MS))
                .signWith(SECRET_KEY)
                .compact();
    }

    /*
     * Parse and validate a JWT token.
     *
     * Returns the Claims object if the token is valid.
     * Throws JwtException if:
     *   - signature doesn't match (tampered token)
     *   - token is expired
     *   - token is malformed
     */
    public static Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
