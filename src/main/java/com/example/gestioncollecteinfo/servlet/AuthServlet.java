package com.example.gestioncollecteinfo.servlet;

import com.example.gestioncollecteinfo.dao.AdminDAO;
import com.example.gestioncollecteinfo.dao.EmployeeDAO;
import com.example.gestioncollecteinfo.model.Admin;
import com.example.gestioncollecteinfo.model.Employee;
import com.example.gestioncollecteinfo.util.JwtUtil;
import com.example.gestioncollecteinfo.util.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet({"/api/auth/login", "/api/auth/logout"})
public class AuthServlet extends BaseServlet {

    private final AdminDAO    adminDAO    = new AdminDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    /*
     * Login flow:
     * 1. Read email + password from request body
     * 2. Look up admin by email → verify password with BCrypt
     * 3. If not admin → look up employee by email → verify password
     * 4. If neither → 401
     *
     * On success, a signed JWT is returned in the response body.
     * The client must store this token and send it as:
     *   Authorization: Bearer <token>
     *
     * IMPORTANT: PasswordUtil.verify() is constant-time — it always
     * runs the full BCrypt comparison even when the hash doesn't match.
     * This prevents timing attacks that could reveal whether an email exists.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Map<String, String> body = mapper.readValue(req.getReader(), Map.class);
        String email    = body.get("email");
        String password = body.get("motDePasse");

        if (email == null || email.isBlank() ||
                password == null || password.isBlank()) {
            sendError(resp, 400, "Email and password are required");
            return;
        }

        // Check admin table
        Admin admin = adminDAO.findByEmail(email);
        if (admin != null) {
            if (!PasswordUtil.verify(password, admin.getMotDePasse())) {
                sendError(resp, 401, "Email ou mot de passe incorrect");
                return;
            }
            String token = JwtUtil.generateToken(admin.getId(), "ADMIN");
            sendJson(resp, buildAuthResponse(admin.getId(), admin.getNom(),
                    admin.getEmail(), "ADMIN", token));
            return;
        }

        // Check employee table
        Employee employee = employeeDAO.findByEmail(email);
        if (employee != null) {
            if (!PasswordUtil.verify(password, employee.getMotDePasse())) {
                sendError(resp, 401, "Email ou mot de passe incorrect");
                return;
            }
            String token = JwtUtil.generateToken(employee.getId(), "EMPLOYEE");
            sendJson(resp, buildAuthResponse(employee.getId(), employee.getNom(),
                    employee.getEmail(), "EMPLOYEE", token));
            return;
        }

        sendError(resp, 401, "Email ou mot de passe incorrect");
    }

    /*
     * Logout — JWT is stateless, so there's nothing to invalidate server-side.
     * The client simply discards the token.
     * This endpoint exists so the frontend has a clean logout URL to call.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        sendJson(resp, Map.of("message", "Logged out successfully"));
    }

    private Map<String, Object> buildAuthResponse(Long id, String nom,
                                                  String email, String role,
                                                  String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("id",    id);
        response.put("nom",   nom);
        response.put("email", email);
        response.put("role",  role);
        response.put("token", token);
        return response;
    }
}