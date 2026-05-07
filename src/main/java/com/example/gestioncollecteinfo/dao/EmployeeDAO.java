package com.example.gestioncollecteinfo.dao;

import com.example.gestioncollecteinfo.model.Employee;
import com.example.gestioncollecteinfo.util.JPAUtil;
import com.example.gestioncollecteinfo.util.PagedResult;
import com.example.gestioncollecteinfo.util.PasswordUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.List;

public class EmployeeDAO {

    // ── CREATE ────────────────────────────────────────────────────
    /*
     * Password is hashed here before persisting.
     * The servlet passes the plain password — the DAO ensures
     * it is never stored in plain text.
     */
    public Employee save(Employee employee) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            employee.setMotDePasse(PasswordUtil.hash(employee.getMotDePasse()));
            em.getTransaction().begin();
            em.persist(employee);
            em.getTransaction().commit();
            em.refresh(employee);
            return employee;
        } finally {
            em.close();
        }
    }

    // ── READ by ID ────────────────────────────────────────────────
    public Employee findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Employee.class, id);
        } finally {
            em.close();
        }
    }

    // ── READ ALL (no pagination) ───────────────────────────────────
    public List<Employee> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT e FROM Employee e JOIN FETCH e.categorie ORDER BY e.nom",
                            Employee.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── READ ALL (paginated) ───────────────────────────────────────
    /*
     * page is zero-based (page=0 is the first page).
     * setFirstResult: how many rows to skip = page * size
     * setMaxResults:  how many rows to return = size
     */
    public PagedResult<Employee> findAllPaged(int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Employee> content = em.createQuery(
                            "SELECT e FROM Employee e JOIN FETCH e.categorie ORDER BY e.nom",
                            Employee.class)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();

            long total = em.createQuery(
                            "SELECT COUNT(e) FROM Employee e", Long.class)
                    .getSingleResult();

            return new PagedResult<>(content, page, size, total);
        } finally {
            em.close();
        }
    }

    // ── READ by email ──────────────────────────────────────────────
    public Employee findByEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT e FROM Employee e WHERE e.email = :email",
                            Employee.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    // ── READ by categorie ──────────────────────────────────────────
    public List<Employee> findByCategorie(Long categorieId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT e FROM Employee e " +
                                    "JOIN FETCH e.categorie c " +
                                    "WHERE c.id = :categorieId",
                            Employee.class)
                    .setParameter("categorieId", categorieId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── READ by project ────────────────────────────────────────────
    public List<Employee> findByProject(Long projetId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT DISTINCT a.employee FROM Affectation a " +
                                    "JOIN FETCH a.employee.categorie " +
                                    "WHERE a.projet.id = :projetId",
                            Employee.class)
                    .setParameter("projetId", projetId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── READ employees NOT in a project ───────────────────────────
    public List<Employee> findEmployeesNotInProject(Long projetId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT e FROM Employee e " +
                                    "JOIN FETCH e.categorie " +
                                    "WHERE e.id NOT IN (" +
                                    "   SELECT a.employee.id FROM Affectation a " +
                                    "   WHERE a.projet.id = :projetId" +
                                    ")",
                            Employee.class)
                    .setParameter("projetId", projetId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── EMAIL UNIQUENESS CHECK ─────────────────────────────────────
    public boolean existsByEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(e) FROM Employee e WHERE e.email = :email",
                            Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    // ── EMAIL UNIQUENESS CHECK (excluding self) ────────────────────
    /*
     * Used when an employee updates their own profile or admin updates
     * an employee. We must allow the employee to keep their own email,
     * so we exclude their own ID from the uniqueness check.
     */
    public boolean existsByEmailExcluding(String email, Long excludeId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(e) FROM Employee e " +
                                    "WHERE e.email = :email AND e.id <> :excludeId",
                            Long.class)
                    .setParameter("email", email)
                    .setParameter("excludeId", excludeId)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    // ── UPDATE (admin — can change categorie) ──────────────────────
    /*
     * Only re-hashes the password if a new plain-text value was sent.
     * If the body contains the existing hash (starts with "$2a$"),
     * we leave it as-is — the admin didn't intend to change the password.
     */
    public Employee update(Employee employee) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            if (employee.getMotDePasse() != null &&
                    !employee.getMotDePasse().startsWith("$2a$")) {
                employee.setMotDePasse(PasswordUtil.hash(employee.getMotDePasse()));
            }
            em.getTransaction().begin();
            Employee updated = em.merge(employee);
            em.getTransaction().commit();
            em.refresh(updated);
            return updated;
        } finally {
            em.close();
        }
    }

    // ── UPDATE SELF (employee — cannot change their own categorie) ──
    /*
     * Used by PUT /api/employees/me.
     * Only updates nom, prenom, email, and optionally password.
     * Categorie is intentionally excluded — only admin can change that.
     */
    public Employee updateSelf(Long id, String nom, String prenom,
                               String email, String newPlainPassword) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Employee existing = em.find(Employee.class, id);

            existing.setNom(nom);
            existing.setPrenom(prenom);
            existing.setEmail(email);

            // Only update password if a new one was provided
            if (newPlainPassword != null && !newPlainPassword.isBlank()) {
                existing.setMotDePasse(PasswordUtil.hash(newPlainPassword));
            }

            em.getTransaction().commit();
            em.refresh(existing);
            return existing;
        } finally {
            em.close();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────
    public void delete(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Employee e = em.find(Employee.class, id);
            if (e != null) em.remove(e);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}