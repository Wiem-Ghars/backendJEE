package com.example.gestioncollecteinfo.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {

    /*
     * Single shared ObjectMapper — expensive to create, so we make one.
     * JavaTimeModule: handles LocalDate serialization → "2024-01-01" not [2024,1,1]
     * WRITE_DATES_AS_TIMESTAMPS disabled: forces ISO string format for dates
     */
    protected static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ── RESPONSE HELPERS ──────────────────────────────────────────

    /*
     * Writes any Java object as JSON to the HTTP response.
     * Jackson handles the conversion automatically.
     */
    protected void sendJson(HttpServletResponse resp, Object data)
            throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        mapper.writeValue(resp.getWriter(), data);
    }

    /*
     * Writes an error response with the given HTTP status code.
     * Always returns JSON — never a plain text error.
     */
    protected void sendError(HttpServletResponse resp, int status, String message)
            throws IOException {
        resp.setStatus(status);
        sendJson(resp, new ErrorMessage(message));
    }

    // ── AUTH HELPERS (JWT-based — reads request attributes) ──────

    /*
     * Checks if the current request has been authenticated by AuthFilter.
     * AuthFilter stores userId and role as request attributes after
     * validating the JWT token from the Authorization header.
     * Returns false and sends 401 automatically if not authenticated.
     */
    protected boolean isAuthenticated(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (req.getAttribute("role") == null) {
            sendError(resp, 401, "Not authenticated");
            return false;
        }
        return true;
    }

    /*
     * Checks if the current request belongs to an ADMIN.
     * Used for endpoints only ADMIN can access.
     * Returns false and sends 401 or 403 automatically if not allowed.
     */
    protected boolean isAdmin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (req.getAttribute("role") == null) {
            sendError(resp, 401, "Not authenticated");
            return false;
        }
        if (!"ADMIN".equals(req.getAttribute("role"))) {
            sendError(resp, 403, "Access denied — admins only");
            return false;
        }
        return true;
    }

    /*
     * Convenience method — gets the role from request attributes.
     * Returns null if the request was not authenticated.
     */
    protected String getRole(HttpServletRequest req) {
        return (String) req.getAttribute("role");
    }

    /*
     * Convenience method — gets the logged-in user's ID from request attributes.
     * Returns null if the request was not authenticated.
     */
    protected Long getSessionUserId(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    // ── ERROR WRAPPER ─────────────────────────────────────────────
    static class ErrorMessage {
        public String error;
        public ErrorMessage(String error) { this.error = error; }
    }
}