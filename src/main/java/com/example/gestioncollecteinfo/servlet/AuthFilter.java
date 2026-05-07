package com.example.gestioncollecteinfo.servlet;

import com.example.gestioncollecteinfo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
 * JWT authentication filter — runs before every /api/* request.
 *
 * Flow:
 *   1. Whitelist check (login, admin seed, OPTIONS, logout)
 *   2. Extract token from "Authorization: Bearer <token>" header
 *   3. Parse & validate via JwtUtil
 *   4. Store userId and role as request attributes for downstream servlets
 *   5. Reject with 401 if token is missing, invalid, or expired
 *
 * Role-based access:
 *   Employees can only GET and PUT (their own profile).
 *   All other methods (POST, DELETE) require ADMIN role.
 */
@WebFilter("/api/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req   = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path   = req.getServletPath();
        String method = req.getMethod();

        // Always allow login
        if (path.equals("/api/auth/login")) {
            chain.doFilter(request, response);
            return;
        }
        // Allow admin creation (seeding only)
        if (path.equals("/api/admins") && method.equals("POST")) {
            chain.doFilter(request, response);
            return;
        }

        // Always allow OPTIONS (CORS preflight)
        if (method.equals("OPTIONS")) {
            chain.doFilter(request, response);
            return;
        }
        if (path.equals("/api/auth/logout")) {
            chain.doFilter(request, response);
            return;
        }

        // ── Extract and validate JWT ──────────────────────────────
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(resp, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7); // strip "Bearer "

        Claims claims;
        try {
            claims = JwtUtil.parseToken(token);
        } catch (JwtException e) {
            sendUnauthorized(resp, "Invalid or expired token");
            return;
        }

        // Store user info as request attributes — servlets read these
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);

        req.setAttribute("userId", userId);
        req.setAttribute("role",   role);

        // Employee can only GET and PUT
        if ("EMPLOYEE".equals(role) && !method.equals("GET") && !method.equals("PUT")) {
            resp.setStatus(403);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Access denied\"}");
            return;
        }

        // All good — let through
        chain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse resp, String message)
            throws IOException {
        resp.setStatus(401);
        resp.setContentType("application/json");
        resp.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}