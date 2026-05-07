package com.example.gestioncollecteinfo.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
 * Runs before EVERY request.
 * Adds CORS headers so Angular (port 4200) can call our API (port 8085).
 * Without these headers the browser blocks all cross-origin requests.
 *
 * With JWT auth, we no longer need Allow-Credentials (no cookies).
 * The Authorization header is explicitly allowed for Bearer tokens.
 */
@WebFilter("/*")
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) response;
        HttpServletRequest req = (HttpServletRequest)  request;

        res.setHeader("Access-Control-Allow-Origin",      "*");
        res.setHeader("Access-Control-Allow-Methods",     "GET, POST, PUT, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers",     "Content-Type, Authorization");

        // Short-circuit OPTIONS — don't let it reach any servlet
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;  // ← do NOT call chain.doFilter
        }

        chain.doFilter(request, response);
    }
}