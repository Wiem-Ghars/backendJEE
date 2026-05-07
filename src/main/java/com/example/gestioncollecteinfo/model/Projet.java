package com.example.gestioncollecteinfo.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "projet")
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    @Column(columnDefinition = "TEXT")
    private String description;

    /*
     * Project start and end dates — set by admin when creating a project.
     * The status (statut) and countdown (joursRestants) are computed
     * automatically from these dates — no manual input needed.
     */
    private LocalDate dateDebut;
    private LocalDate dateFin;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    /*
     * COMPUTED STATUS — not stored in the database.
     *
     * Automatically derived from dateDebut and dateFin:
     *   today < dateDebut                        → EN_ATTENTE
     *   dateDebut <= today <= dateFin             → EN_COURS
     *   today > dateFin                           → TERMINE
     *   dateDebut set but dateFin missing         → EN_COURS (open-ended)
     *   neither date set                          → EN_ATTENTE (fallback)
     *
     * @Transient tells JPA to NOT persist this — it's always fresh.
     * Jackson serializes it automatically via the getter.
     */
    @Transient
    public Statut getStatut() {
        LocalDate today = LocalDate.now();

        if (dateDebut == null) return Statut.EN_ATTENTE;
        if (today.isBefore(dateDebut)) return Statut.EN_ATTENTE;
        if (dateFin == null) return Statut.EN_COURS; // started, no end date
        if (today.isAfter(dateFin)) return Statut.TERMINE;
        return Statut.EN_COURS;
    }



    /*
     * Calculated countdown — NOT stored in the database.
     *
     * Returns:
     *   - null if dateFin is not set
     *   - 0 if the project is overdue or ends today
     *   - positive number = days remaining until dateFin
     */
    @Transient
    public Long getJoursRestants() {
        if (dateFin == null) return null;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), dateFin);
        return Math.max(days, 0);
    }
}