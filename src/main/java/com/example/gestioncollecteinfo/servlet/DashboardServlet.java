package com.example.gestioncollecteinfo.servlet;

import com.example.gestioncollecteinfo.dao.StatsDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
 * GET /api/dashboard
 * Admin only — returns all aggregate stats the dashboard needs in one call.
 *
 * Response shape:
 * {
 *   "totalEmployees":       12,
 *   "totalProjets":          7,
 *   "totalCategories":       4,
 *   "activeAffectations":    5,
 *   "projetsEnAttente":      2,
 *   "projetsEnCours":        3,
 *   "projetsTermines":       2,
 *   "employeesPerCategory": [
 *     { "categorie": "Ingénieur", "count": 5 },
 *     { "categorie": "Technicien", "count": 4 },
 *     ...
 *   ]
 * }
 *
 * Why a dedicated endpoint instead of calling /employees, /projets etc?
 *   - One HTTP request instead of 4+
 *   - No data leakage — the frontend gets counts, not full entity lists
 *   - Aggregation done in the DB (efficient) not in the browser (wasteful)
 */
@WebServlet("/api/dashboard")
public class DashboardServlet extends BaseServlet {

    private final StatsDAO statsDAO = new StatsDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!isAdmin(req, resp)) return;
        sendJson(resp, statsDAO.getDashboardStats());
    }
}
