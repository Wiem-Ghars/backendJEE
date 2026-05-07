package com.example.gestioncollecteinfo.servlet;

import com.example.gestioncollecteinfo.dao.AffectationDAO;
import com.example.gestioncollecteinfo.dao.ProjetDAO;
import com.example.gestioncollecteinfo.model.Projet;
import com.example.gestioncollecteinfo.model.Statut;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/projets/*")
public class ProjetServlet extends BaseServlet {

    private final ProjetDAO      dao            = new ProjetDAO();
    private final AffectationDAO affectationDAO = new AffectationDAO();

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE     = 100;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAuthenticated(req, resp)) return;

        String pathInfo = req.getPathInfo();

        // GET /api/projets/mine → employee sees only their assigned projects
        // No pagination here — employees are typically on few projects
        if (pathInfo != null && pathInfo.equals("/mine")) {
            Long employeeId = getSessionUserId(req);
            sendJson(resp, dao.findByEmployee(employeeId));
            return;
        }

        if (!isAdmin(req, resp)) return;

        if (pathInfo == null || pathInfo.equals("/")) {
            int page = parseIntParam(req, "page", DEFAULT_PAGE);
            int size = parseIntParam(req, "size", DEFAULT_SIZE);
            size = Math.min(size, MAX_SIZE);

            String statutParam = req.getParameter("statut");
            if (statutParam != null) {
                // GET /api/projets?statut=EN_COURS&page=0&size=10
                // Status is computed from dates, but we can still filter by it
                try {
                    Statut statut = Statut.valueOf(statutParam.toUpperCase());
                    sendJson(resp, dao.findByStatutPaged(statut, page, size));
                } catch (IllegalArgumentException e) {
                    sendError(resp, 400,
                            "Invalid statut value. Allowed: EN_ATTENTE, EN_COURS, TERMINE");
                }
            } else {
                // GET /api/projets?page=0&size=10
                sendJson(resp, dao.findAllPaged(page, size));
            }
        } else {
            // GET /api/projets/{id}
            try {
                Long id = Long.parseLong(pathInfo.substring(1));
                Projet p = dao.findById(id);
                if (p == null) sendError(resp, 404, "Projet not found");
                else sendJson(resp, p);
            } catch (NumberFormatException e) {
                sendError(resp, 400, "Invalid project id");
            }
        }
    }

    /*
     * POST /api/projets — admin creates a new project.
     *
     * Required: nom, dateDebut
     * Optional: description, dateFin
     *
     * Status is NOT set manually — it's computed from dates:
     *   today < dateDebut → EN_ATTENTE
     *   dateDebut <= today → EN_COURS
     *   today > dateFin   → TERMINE
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        Projet projet = mapper.readValue(req.getReader(), Projet.class);

        if (projet.getNom() == null || projet.getNom().isBlank()) {
            sendError(resp, 400, "Nom is required");
            return;
        }

        // Validate dates — dateDebut required
        if (projet.getDateDebut() == null) {
            sendError(resp, 400, "dateDebut is required");
            return;
        }
        // dateFin must be on or after dateDebut if provided
        if (projet.getDateFin() != null &&
                projet.getDateFin().isBefore(projet.getDateDebut())) {
            sendError(resp, 400, "dateFin must be on or after dateDebut");
            return;
        }


        Projet saved = dao.save(projet);
        resp.setStatus(201);
        sendJson(resp, saved);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        Long id;
        try {
            id = Long.parseLong(req.getPathInfo().substring(1));
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid project id");
            return;
        }

        Projet existing = dao.findById(id);
        if (existing == null) { sendError(resp, 404, "Projet not found"); return; }

        Projet projet = mapper.readValue(req.getReader(), Projet.class);
        projet.setId(id);

        // Preserve existing dates if not provided in update
        if (projet.getDateDebut() == null) projet.setDateDebut(existing.getDateDebut());
        if (projet.getDateFin() == null) projet.setDateFin(existing.getDateFin());

        // Validate date consistency
        if (projet.getDateFin() != null && projet.getDateDebut() != null &&
                projet.getDateFin().isBefore(projet.getDateDebut())) {
            sendError(resp, 400, "dateFin must be on or after dateDebut");
            return;
        }

        sendJson(resp, dao.update(projet));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        Long id;
        try {
            id = Long.parseLong(req.getPathInfo().substring(1));
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid project id");
            return;
        }

        if (dao.findById(id) == null) { sendError(resp, 404, "Projet not found"); return; }

        if (!affectationDAO.findByProjet(id).isEmpty()) {
            sendError(resp, 409, "Cannot delete — employees are assigned to this project");
            return;
        }

        dao.delete(id);
        resp.setStatus(204);
    }

    private int parseIntParam(HttpServletRequest req, String name, int defaultValue) {
        String val = req.getParameter(name);
        if (val == null) return defaultValue;
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}