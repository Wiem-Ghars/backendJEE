package com.example.gestioncollecteinfo.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // This runs when Tomcat starts — forces JPA to initialize
        // and create the tables immediately
        JPAUtil.getEntityManager().close();
        System.out.println("✅ JPA initialized — tables created!");
    }
}