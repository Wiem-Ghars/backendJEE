package com.example.gestioncollecteinfo.util;

import jakarta.persistence.*;

public class JPAUtil {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("gestionPU");

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
}