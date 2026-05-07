package com.example.gestioncollecteinfo.dao;

import com.example.gestioncollecteinfo.model.Admin;
import com.example.gestioncollecteinfo.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import com.example.gestioncollecteinfo.util.PasswordUtil;
public class AdminDAO {

    // ── CREATE ────────────────────────────────────────────────────
    /*
     * Used once to seed the first admin account.
     * Password check is NOT done here — AuthServlet handles that.
     */
    public Admin save(Admin admin) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Hash the password before saving — never store plain text
            admin.setMotDePasse(PasswordUtil.hash(admin.getMotDePasse()));

            em.getTransaction().begin();
            em.persist(admin);
            em.getTransaction().commit();
            return admin;
        } finally {
            em.close();
        }
    }

    // ── READ by email (used at login) ─────────────────────────────
    /*
     * Returns the Admin object if found, null if not.
     * The Servlet — NOT this method — is responsible for
     * checking if the password matches.
     * This keeps the DAO focused on ONE job: finding data.
     */
    public Admin findByEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT a FROM Admin a WHERE a.email = :email",
                            Admin.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null; // no admin with this email — login will fail
        } finally {
            em.close();
        }
    }

    // ── EMAIL UNIQUENESS CHECK ────────────────────────────────────
    /*
     * Returns true if an admin with this email already exists.
     * Called by AdminServlet before saving to prevent duplicates.
     * Uses COUNT for efficiency — no need to load the full object.
     */
    public boolean existsByEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(a) FROM Admin a WHERE a.email = :email",
                            Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
}