package com.example.gestioncollecteinfo.dao;

import com.example.gestioncollecteinfo.model.Categorie;
import com.example.gestioncollecteinfo.util.JPAUtil;
import com.example.gestioncollecteinfo.util.PagedResult;
import jakarta.persistence.EntityManager;
import java.util.List;

public class CategorieDAO {

    // ── CREATE ────────────────────────────────────────────────────
    public Categorie save(Categorie categorie) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(categorie);
            em.getTransaction().commit();
            return categorie;
        } finally {
            em.close();
        }
    }

    // ── READ ALL (no pagination) ───────────────────────────────────
    public List<Categorie> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Categorie c ORDER BY c.nom",
                            Categorie.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── READ ALL (paginated) ───────────────────────────────────────
    public PagedResult<Categorie> findAllPaged(int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Categorie> content = em.createQuery(
                            "SELECT c FROM Categorie c ORDER BY c.nom",
                            Categorie.class)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();

            long total = em.createQuery(
                            "SELECT COUNT(c) FROM Categorie c", Long.class)
                    .getSingleResult();

            return new PagedResult<>(content, page, size, total);
        } finally {
            em.close();
        }
    }

    // ── READ by ID ────────────────────────────────────────────────
    public Categorie findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Categorie.class, id);
        } finally {
            em.close();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────
    public Categorie update(Categorie categorie) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Categorie updated = em.merge(categorie);
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
            Categorie c = em.find(Categorie.class, id);
            if (c != null) em.remove(c);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}