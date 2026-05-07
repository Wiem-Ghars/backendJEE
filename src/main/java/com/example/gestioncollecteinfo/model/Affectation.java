package com.example.gestioncollecteinfo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "affectation", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "projet_id"})
})
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;

    /*
     * Required by project spec:
     * "Chaque affectation possède une date de début et une date de fin"
     */
    private LocalDate dateDebut;
    private LocalDate dateFin;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee e) { this.employee = e; }
    public Projet getProjet() { return projet; }
    public void setProjet(Projet p) { this.projet = p; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
}