package com.example.gestioncollecteinfo.util;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

public class JPAUtil {

    private static final EntityManagerFactory emf = buildEntityManagerFactory();

    private static EntityManagerFactory buildEntityManagerFactory() {
        Map<String, String> props = new HashMap<>();

        String dbUrl = System.getenv("DATABASE_URL");
        String dbUser = System.getenv("DATABASE_USER");
        String dbPassword = System.getenv("DATABASE_PASSWORD");

        if (dbUrl != null) props.put("jakarta.persistence.jdbc.url", dbUrl);
        if (dbUser != null) props.put("jakarta.persistence.jdbc.user", dbUser);
        if (dbPassword != null) props.put("jakarta.persistence.jdbc.password", dbPassword);

        return Persistence.createEntityManagerFactory("gestionPU", props);
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
}