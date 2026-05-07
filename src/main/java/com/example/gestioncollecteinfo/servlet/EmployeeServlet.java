package com.example.gestioncollecteinfo.servlet;

import com.example.gestioncollecteinfo.dao.AdminDAO;
import com.example.gestioncollecteinfo.dao.CategorieDAO;
import com.example.gestioncollecteinfo.dao.EmployeeDAO;
import com.example.gestioncollecteinfo.model.Employee;
import com.example.gestioncollecteinfo.util.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/employees/*")
public class EmployeeServlet extends BaseServlet {

    private final EmployeeDAO  dao          = new EmployeeDAO();
    private final AdminDAO     adminDAO     = new AdminDAO();
    private final CategorieDAO categorieDAO = new CategorieDAO();

    // ── DEFAULT PAGINATION CONSTANTS ──────────────────────────────
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE     = 100; // prevent abuse

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAuthenticated(req, resp)) return;

        String pathInfo = req.getPathInfo();

        // GET /api/employees/me → employee fetches their own profile
        if (pathInfo != null && pathInfo.equals("/me")) {
            Long userId = getSessionUserId(req);
            Employee e = dao.findById(userId);
            if (e == null) { sendError(resp, 404, "Employee not found"); return; }
            sendJson(resp, sanitize(e));
            return;
        }

        // All other GET routes → admin only
        if (!isAdmin(req, resp)) return;

        // GET /api/employees?categorieId=1 → filter by category (no pagination needed here)
        String categorieParam = req.getParameter("categorieId");
        if (categorieParam != null) {
            try {
                Long categorieId = Long.parseLong(categorieParam);
                sendJson(resp, dao.findByCategorie(categorieId));
            } catch (NumberFormatException e) {
                sendError(resp, 400, "Invalid categorieId");
            }
            return;
        }

        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /api/employees?page=0&size=10 → paginated list
            int page = parseIntParam(req, "page", DEFAULT_PAGE);
            int size = parseIntParam(req, "size", DEFAULT_SIZE);
            size = Math.min(size, MAX_SIZE); // cap to prevent huge responses
            sendJson(resp, dao.findAllPaged(page, size));
        } else {
            // GET /api/employees/{id}
            try {
                Long id = Long.parseLong(pathInfo.substring(1));
                Employee e = dao.findById(id);
                if (e == null) sendError(resp, 404, "Employee not found");
                else sendJson(resp, sanitize(e));
            } catch (NumberFormatException e) {
                sendError(resp, 400, "Invalid employee id");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        Employee employee = mapper.readValue(req.getReader(), Employee.class);

        if (dao.existsByEmail(employee.getEmail())) {
            sendError(resp, 409, "Un employé avec cet email existe déjà");
            return;
        }

        if (employee.getNom() == null || employee.getNom().isBlank() ||
                employee.getEmail() == null || employee.getEmail().isBlank() ||
                employee.getMotDePasse() == null || employee.getMotDePasse().isBlank()) {
            sendError(resp, 400, "Nom, email and password are required");
            return;
        }
        if (employee.getCategorie() == null || employee.getCategorie().getId() == null) {
            sendError(resp, 400, "Categorie is required");
            return;
        }
        if (categorieDAO.findById(employee.getCategorie().getId()) == null) {
            sendError(resp, 404, "Categorie not found");
            return;
        }
        if (adminDAO.existsByEmail(employee.getEmail()) ||
                dao.existsByEmail(employee.getEmail())) {
            sendError(resp, 409, "An account with this email already exists");
            return;
        }

        // DAO hashes the password before persisting
        Employee saved = dao.save(employee);
        resp.setStatus(201);
        sendJson(resp, sanitize(saved));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAuthenticated(req, resp)) return;

        String pathInfo = req.getPathInfo();

        // PUT /api/employees/me → employee updates their own profile
        if (pathInfo != null && pathInfo.equals("/me")) {
            handleSelfUpdate(req, resp);
            return;
        }

        // All other PUT routes → admin only
        if (!isAdmin(req, resp)) return;

        Long id;
        try {
            id = Long.parseLong(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid employee id");
            return;
        }

        if (dao.findById(id) == null) {
            sendError(resp, 404, "Employee not found");
            return;
        }

        Employee employee = mapper.readValue(req.getReader(), Employee.class);
        employee.setId(id);

        // Check email uniqueness — allow keeping their own email
        if (employee.getEmail() != null &&
                dao.existsByEmailExcluding(employee.getEmail(), id)) {
            sendError(resp, 409, "An account with this email already exists");
            return;
        }

        // DAO handles conditional re-hashing (only if plain password sent)
        Employee updated = dao.update(employee);
        sendJson(resp, sanitize(updated));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        Long id;
        try {
            id = Long.parseLong(req.getPathInfo().substring(1));
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid employee id");
            return;
        }

        if (dao.findById(id) == null) {
            sendError(resp, 404, "Employee not found");
            return;
        }

        dao.delete(id);
        resp.setStatus(204);
    }

    // ── SELF-UPDATE HANDLER ───────────────────────────────────────
    /*
     * Employee can update: nom, prenom, email, password.
     * They CANNOT change their own categorie — admin territory.
     * If changing password, they must provide currentPassword for verification.
     */
    private void handleSelfUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Long userId = getSessionUserId(req);
        Employee existing = dao.findById(userId);
        if (existing == null) { sendError(resp, 404, "Employee not found"); return; }

        // Read update body as a map so we can handle optional fields safely
        Map<String, String> body = mapper.readValue(req.getReader(), Map.class);

        String nom     = body.get("nom");
        String prenom  = body.get("prenom");
        String email   = body.get("email");
        String current = body.get("currentPassword");
        String newPass = body.get("newPassword");

        // Validate required fields
        if (nom == null || nom.isBlank()) {
            sendError(resp, 400, "Nom is required"); return;
        }
        if (email == null || email.isBlank()) {
            sendError(resp, 400, "Email is required"); return;
        }

        // Check email uniqueness — allow keeping their own email
        if (dao.existsByEmailExcluding(email, userId) ||
                adminDAO.existsByEmail(email)) {
            sendError(resp, 409, "An account with this email already exists"); return;
        }

        // If they want to change their password, verify the current one first
        String passwordToSet = null;
        if (newPass != null && !newPass.isBlank()) {
            if (current == null || current.isBlank()) {
                sendError(resp, 400, "currentPassword is required to set a new password"); return;
            }
            if (!PasswordUtil.verify(current, existing.getMotDePasse())) {
                sendError(resp, 401, "Current password is incorrect"); return;
            }
            passwordToSet = newPass;
        }

        Employee updated = dao.updateSelf(userId, nom, prenom, email, passwordToSet);
        sendJson(resp, sanitize(updated));
    }

    // ── HELPERS ───────────────────────────────────────────────────

    /*
     * Never return the password hash in any API response.
     * We null it out before serializing — the hash is useless to
     * the frontend and shouldn't travel over the wire.
     */
    private Employee sanitize(Employee e) {
        e.setMotDePasse(null);
        return e;
    }

    /*
     * Parse an integer query parameter with a fallback default.
     * Returns the default if the parameter is missing or not a valid integer.
     */
    private int parseIntParam(HttpServletRequest req, String name, int defaultValue) {
        String val = req.getParameter(name);
        if (val == null) return defaultValue;
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}