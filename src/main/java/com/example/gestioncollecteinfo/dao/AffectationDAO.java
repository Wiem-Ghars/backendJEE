package com.example.gestioncollecteinfo.dao;

import com.example.gestioncollecteinfo.model.Affectation;
import com.example.gestioncollecteinfo.util.JPAUtil;
import jakarta.persistence.EntityManager;
import java.util.List;

public class AffectationDAO {

    // ── CREATE ────────────────────────────────────────────────────
    public Affectation save(Affectation affectation) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(affectation);
            em.getTransaction().commit();
            em.refresh(affectation); // reload so employee and projet are fully populated
            return affectation;
        } finally {
            em.close();
        }
    }

    // ── READ ALL ──────────────────────────────────────────────────
    public List<Affectation> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT a FROM Affectation a " +
                                    "JOIN FETCH a.employee e " +
                                    "JOIN FETCH e.categorie " +
                                    "JOIN FETCH a.projet",
                            Affectation.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── READ by project ───────────────────────────────────────────
    public List<Affectation> findByProjet(Long projetId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT a FROM Affectation a " +
                                    "JOIN FETCH a.employee e " +
                                    "JOIN FETCH e.categorie " +
                                    "WHERE a.projet.id = :projetId",
                            Affectation.class)
                    .setParameter("projetId", projetId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── EXISTS by ID ──────────────────────────────────────────────
    /*
     * Used by the servlet before DELETE so we can return a proper 404
     * instead of silently returning 204 on a non-existent record.
     */
    public boolean existsById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(a) FROM Affectation a WHERE a.id = :id",
                            Long.class)
                    .setParameter("id", id)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────
    public void delete(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Affectation a = em.find(Affectation.class, id);
            if (a != null) em.remove(a);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}