package com.example.gestioncollecteinfo.servlet;

import com.example.gestioncollecteinfo.dao.CategorieDAO;
import com.example.gestioncollecteinfo.dao.EmployeeDAO;
import com.example.gestioncollecteinfo.model.Categorie;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/categories/*")
public class CategorieServlet extends BaseServlet {

    private final CategorieDAO dao         = new CategorieDAO();
    private final EmployeeDAO  employeeDAO = new EmployeeDAO();

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE     = 100;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /api/categories?page=0&size=10
            int page = parseIntParam(req, "page", DEFAULT_PAGE);
            int size = parseIntParam(req, "size", DEFAULT_SIZE);
            size = Math.min(size, MAX_SIZE);
            sendJson(resp, dao.findAllPaged(page, size));
        } else {
            // GET /api/categories/{id}
            try {
                Long id = Long.parseLong(pathInfo.substring(1));
                Categorie c = dao.findById(id);
                if (c == null) sendError(resp, 404, "Categorie not found");
                else sendJson(resp, c);
            } catch (NumberFormatException e) {
                sendError(resp, 400, "Invalid category id");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        Categorie categorie = mapper.readValue(req.getReader(), Categorie.class);

        if (categorie.getNom() == null || categorie.getNom().isBlank()) {
            sendError(resp, 400, "Nom is required");
            return;
        }

        Categorie saved = dao.save(categorie);
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
            sendError(resp, 400, "Invalid category id");
            return;
        }

        if (dao.findById(id) == null) {
            sendError(resp, 404, "Categorie not found");
            return;
        }

        Categorie categorie = mapper.readValue(req.getReader(), Categorie.class);
        categorie.setId(id);

        sendJson(resp, dao.update(categorie));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;

        Long id;
        try {
            id = Long.parseLong(req.getPathInfo().substring(1));
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid category id");
            return;
        }

        if (!employeeDAO.findByCategorie(id).isEmpty()) {
            sendError(resp, 409, "Cannot delete — employees are assigned to this category");
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