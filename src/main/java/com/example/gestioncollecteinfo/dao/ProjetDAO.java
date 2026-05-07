package com.example.gestioncollecteinfo.dao;

import com.example.gestioncollecteinfo.model.Projet;
import com.example.gestioncollecteinfo.model.Statut;
import com.example.gestioncollecteinfo.util.JPAUtil;
import com.example.gestioncollecteinfo.util.PagedResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;

public class ProjetDAO {

    // ── CREATE ────────────────────────────────────────────────────
    public Projet save(Projet projet) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(projet);
            em.getTransaction().commit();
            return projet;
        } finally {
            em.close();
        }
    }

    // ── READ ALL (no pagination) ───────────────────────────────────
    public List<Projet> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Projet p ORDER BY p.nom",
                            Projet.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── READ ALL (paginated) ───────────────────────────────────────
    public PagedResult<Projet> findAllPaged(int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Projet> content = em.createQuery(
                            "SELECT p FROM Projet p ORDER BY p.nom",
                            Projet.class)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();

            long total = em.createQuery(
                            "SELECT COUNT(p) FROM Projet p", Long.class)
                    .getSingleResult();

            return new PagedResult<>(content, page, size, total);
        } finally {
            em.close();
        }
    }

    // ── READ by status (no pagination) ────────────────────────────
    /*
     * Since statut is now computed from dates (not a DB column),
     * we translate the requested status into date-based conditions:
     *
     *   EN_ATTENTE → today < dateDebut
     *   EN_COURS   → dateDebut <= today AND (dateFin IS NULL OR today <= dateFin)
     *   TERMINE    → dateFin IS NOT NULL AND today > dateFin
     */
    public List<Projet> findByStatut(Statut statut) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = buildStatutQuery("SELECT p FROM Projet p", statut,
                    " ORDER BY p.nom");
            TypedQuery<Projet> q = em.createQuery(jpql, Projet.class);
            setDateParams(q);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    // ── READ by status (paginated) ─────────────────────────────────
    public PagedResult<Projet> findByStatutPaged(Statut statut, int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = buildStatutQuery("SELECT p FROM Projet p", statut,
                    " ORDER BY p.nom");
            TypedQuery<Projet> q = em.createQuery(jpql, Projet.class);
            setDateParams(q);
            List<Projet> content = q
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();

            String countJpql = buildStatutQuery("SELECT COUNT(p) FROM Projet p",
                    statut, "");
            TypedQuery<Long> cq = em.createQuery(countJpql, Long.class);
            setDateParams(cq);
            long total = cq.getSingleResult();

            return new PagedResult<>(content, page, size, total);
        } finally {
            em.close();
        }
    }

    // ── READ by ID ────────────────────────────────────────────────
    public Projet findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Projet.class, id);
        } finally {
            em.close();
        }
    }

    // ── READ by employee ──────────────────────────────────────────
    public List<Projet> findByEmployee(Long employeeId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT DISTINCT a.projet FROM Affectation a " +
                                    "WHERE a.employee.id = :employeeId " +
                                    "ORDER BY a.projet.nom",
                            Projet.class)
                    .setParameter("employeeId", employeeId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────
    public Projet update(Projet projet) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Projet updated = em.merge(projet);
            em.getTransaction().commit();
            return updated;
        } finally {
            em.close();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────
    public void delete(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Projet p = em.find(Projet.class, id);
            if (p != null) em.remove(p);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────

    /*
     * Builds a JPQL WHERE clause that translates a Statut enum
     * into date-based conditions (since statut is no longer a DB column).
     */
    private String buildStatutQuery(String select, Statut statut, String orderBy) {
        String where;
        switch (statut) {
            case EN_ATTENTE:
                where = " WHERE (p.dateDebut IS NULL OR p.dateDebut > :today)";
                break;
            case EN_COURS:
                where = " WHERE p.dateDebut IS NOT NULL AND p.dateDebut <= :today" +
                        " AND (p.dateFin IS NULL OR p.dateFin >= :today)";
                break;
            case TERMINE:
                where = " WHERE p.dateFin IS NOT NULL AND p.dateFin < :today";
                break;
            default:
                where = "";
        }
        return select + where + orderBy;
    }

    private void setDateParams(TypedQuery<?> q) {
        try {
            q.setParameter("today", LocalDate.now());
        } catch (IllegalArgumentException ignored) {
            // query doesn't have :today param (shouldn't happen)
        }
    }
}