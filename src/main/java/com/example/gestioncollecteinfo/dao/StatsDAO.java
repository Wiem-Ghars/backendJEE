package com.example.gestioncollecteinfo.dao;

import com.example.gestioncollecteinfo.model.Projet;
import com.example.gestioncollecteinfo.util.JPAUtil;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsDAO {

    public Map<String, Object> getDashboardStats() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Map<String, Object> stats = new LinkedHashMap<>();
            LocalDate today = LocalDate.now();

            // ── Counts ────────────────────────────────────────────
            stats.put("totalEmployees",
                    em.createQuery("SELECT COUNT(e) FROM Employee e", Long.class)
                            .getSingleResult());

            stats.put("totalProjets",
                    em.createQuery("SELECT COUNT(p) FROM Projet p", Long.class)
                            .getSingleResult());

            stats.put("totalCategories",
                    em.createQuery("SELECT COUNT(c) FROM Categorie c", Long.class)
                            .getSingleResult());

            // ── Unassigned employees (no affectation at all) ──────
            stats.put("unassignedEmployees",
                    em.createQuery(
                                    "SELECT COUNT(e) FROM Employee e " +
                                            "WHERE e.id NOT IN (" +
                                            "  SELECT DISTINCT a.employee.id FROM Affectation a" +
                                            ")",
                                    Long.class)
                            .getSingleResult());

            // ── Projects by status (computed from dates) ──────────
            /*
             * Since statut is no longer a DB column, we use date conditions:
             *   EN_ATTENTE → dateDebut is null OR today < dateDebut
             *   EN_COURS   → dateDebut <= today AND (dateFin is null OR today <= dateFin)
             *   TERMINE    → dateFin is not null AND today > dateFin
             */
            stats.put("projetsEnAttente",
                    em.createQuery(
                                    "SELECT COUNT(p) FROM Projet p " +
                                            "WHERE p.dateDebut IS NULL OR p.dateDebut > :today",
                                    Long.class)
                            .setParameter("today", today)
                            .getSingleResult());

            stats.put("projetsEnCours",
                    em.createQuery(
                                    "SELECT COUNT(p) FROM Projet p " +
                                            "WHERE p.dateDebut IS NOT NULL AND p.dateDebut <= :today " +
                                            "AND (p.dateFin IS NULL OR p.dateFin >= :today)",
                                    Long.class)
                            .setParameter("today", today)
                            .getSingleResult());

            stats.put("projetsTermines",
                    em.createQuery(
                                    "SELECT COUNT(p) FROM Projet p " +
                                            "WHERE p.dateFin IS NOT NULL AND p.dateFin < :today",
                                    Long.class)
                            .setParameter("today", today)
                            .getSingleResult());

            // ── Employees per category ────────────────────────────
            List<Object[]> rawCatStats = em.createQuery(
                            "SELECT c.nom, COUNT(e) FROM Employee e " +
                                    "JOIN e.categorie c " +
                                    "GROUP BY c.nom " +
                                    "ORDER BY COUNT(e) DESC",
                            Object[].class)
                    .getResultList();

            List<Map<String, Object>> employeesPerCategory = rawCatStats.stream()
                    .map(row -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("categorie", row[0]);
                        entry.put("count", row[1]);
                        return entry;
                    })
                    .collect(Collectors.toList());

            stats.put("employeesPerCategory", employeesPerCategory);

            // ── Projects ending soon (within 30 days) ─────────────
            /*
             * Returns active (not yet finished) projects whose dateFin
             * is between today and 30 days from now, sorted by
             * closest deadline first.
             *
             * Each Projet object has getJoursRestants() and getStatut()
             * which Jackson serializes automatically, so the response
             * includes the countdown and status for free.
             */
            List<Projet> endingSoon = em.createQuery(
                            "SELECT p FROM Projet p " +
                                    "WHERE p.dateFin IS NOT NULL " +
                                    "  AND p.dateFin >= :today " +
                                    "  AND p.dateFin <= :deadline " +
                                    "  AND p.dateDebut IS NOT NULL " +
                                    "  AND p.dateDebut <= :today " +
                                    "ORDER BY p.dateFin ASC",
                            Projet.class)
                    .setParameter("today", today)
                    .setParameter("deadline", today.plusDays(30))
                    .getResultList();

            stats.put("projetsEndingSoon", endingSoon);

            return stats;
        } finally {
            em.close();
        }
    }
}