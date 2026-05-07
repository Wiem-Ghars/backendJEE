package com.example.gestioncollecteinfo.servlet;

import com.example.gestioncollecteinfo.dao.AdminDAO;
import com.example.gestioncollecteinfo.model.Admin;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/*
 * Only one endpoint: POST /api/admins (seeding the first admin).
 * This endpoint is exempted from AuthFilter so it works without a session.
 * In a real system you would remove or protect this after first use.
 */
@WebServlet("/api/admins")
public class AdminServlet extends BaseServlet {

    private final AdminDAO dao = new AdminDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Admin admin = mapper.readValue(req.getReader(), Admin.class);

        // Validate required fields
        if (admin.getNom() == null || admin.getNom().isBlank() ||
                admin.getEmail() == null || admin.getEmail().isBlank() ||
                admin.getMotDePasse() == null || admin.getMotDePasse().isBlank()) {
            sendError(resp, 400, "Nom, email and password are required");
            return;
        }

        // Check email not already used by another admin
        if (dao.existsByEmail(admin.getEmail())) {
            sendError(resp, 409, "An admin with this email already exists");
            return;
        }

        Admin saved = dao.save(admin);
        resp.setStatus(201);

        // Return safe response — never return the password
        Map<String, Object> response = new HashMap<>();
        response.put("id",    saved.getId());
        response.put("nom",   saved.getNom());
        response.put("email", saved.getEmail());
        sendJson(resp, response);
    }
}