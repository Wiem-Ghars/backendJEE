package com.example.gestioncollecteinfo.servlet;

import com.example.gestioncollecteinfo.dao.AffectationDAO;
import com.example.gestioncollecteinfo.dao.ProjetDAO;
import com.example.gestioncollecteinfo.model.Affectation;
import com.example.gestioncollecteinfo.model.Projet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/affectations/*")
public class AffectationServlet extends BaseServlet {

    private final AffectationDAO dao = new AffectationDAO();
    private final ProjetDAO projetDAO = new ProjetDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAuthenticated(req, resp)) return;

        String pathInfo = req.getPathInfo();

        // GET /api/affectations/projet/{id} — admin and employee
        if (pathInfo != null && pathInfo.startsWith("/projet/")) {
            try {
                Long projetId = Long.parseLong(pathInfo.substring(8));
                sendJson(resp, dao.findByProjet(projetId));
            } catch (NumberFormatException e) {
                sendError(resp, 400, "Invalid project id");
            }
            return;
        }

        // GET /api/affectations — admin only
        if (!isAdmin(req, resp)) return;
        sendJson(resp, dao.findAll());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        Affectation affectation = mapper.readValue(req.getReader(), Affectation.class);

        // Validate employee and project references
        if (affectation.getEmployee() == null ||
                affectation.getEmployee().getId() == null) {
            sendError(resp, 400, "Employee is required");
            return;
        }
        if (affectation.getProjet() == null ||
                affectation.getProjet().getId() == null) {
            sendError(resp, 400, "Projet is required");
            return;
        }

        // Validate dates — dateDebut is required per project spec
        if (affectation.getDateDebut() == null) {
            sendError(resp, 400, "dateDebut is required");
            return;
        }
        // dateFin must be on or after dateDebut if provided
        if (affectation.getDateFin() != null &&
                affectation.getDateFin().isBefore(affectation.getDateDebut())) {
            sendError(resp, 400, "dateFin must be on or after dateDebut");
            return;
        }

        // Duplicate assignment check
        Long projetId   = affectation.getProjet().getId();
        Long employeeId = affectation.getEmployee().getId();

        Projet projet = projetDAO.findById(projetId);
        if (projet == null) {
            sendError(resp, 404, "Projet introuvable");
            return;
        }

        // Validate affectation dates against project dates
        if (affectation.getDateDebut().isBefore(projet.getDateDebut())) {
            sendError(resp, 400, "La date de début de l'affectation ne peut pas être avant le début du projet.");
            return;
        }
        if (projet.getDateFin() != null) {
            if (affectation.getDateDebut().isAfter(projet.getDateFin())) {
                sendError(resp, 400, "L'affectation ne peut pas commencer après la fin du projet.");
                return;
            }
            if (affectation.getDateFin() != null && affectation.getDateFin().isAfter(projet.getDateFin())) {
                sendError(resp, 400, "L'affectation ne peut pas dépasser la date de fin du projet.");
                return;
            }
        }
        boolean alreadyAssigned = dao.findByProjet(projetId)
                .stream()
                .anyMatch(a -> a.getEmployee().getId().equals(employeeId));

        if (alreadyAssigned) {
            sendError(resp, 409, "This employee is already assigned to this project");
            return;
        }

        Affectation saved = dao.save(affectation);
        resp.setStatus(201);
        sendJson(resp, saved);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        Long id;
        try {
            id = Long.parseLong(req.getPathInfo().substring(1));
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid affectation id");
            return;
        }

        if (!dao.existsById(id)) {
            sendError(resp, 404, "Affectation not found");
            return;
        }

        dao.delete(id);
        resp.setStatus(204);
    }
}