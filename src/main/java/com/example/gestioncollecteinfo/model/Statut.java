package com.example.gestioncollecteinfo.model;

/*
 * Three possible states for a project.
 *
 * Stored as a STRING in the DB (not an integer ordinal) so the column
 * stays readable and adding/reordering values never corrupts existing rows.
 *
 * EN_ATTENTE → project created but work hasn't started yet
 * EN_COURS   → actively being worked on
 * TERMINE    → completed
 */
public enum Statut {
    EN_ATTENTE,
    EN_COURS,
    TERMINE
}
