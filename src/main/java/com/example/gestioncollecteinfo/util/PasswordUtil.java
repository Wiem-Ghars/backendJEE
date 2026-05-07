package com.example.gestioncollecteinfo.util;

import org.mindrot.jbcrypt.BCrypt;

/*
 * Single utility class for all password operations.
 * All hashing and verification go through here — never call BCrypt directly
 * from a servlet or DAO. This way, if you ever swap algorithms, you change
 * one file.
 *
 * How BCrypt works:
 *   - hash()   → takes a plain password, returns a 60-char hash string
 *   - verify() → takes plain password + stored hash, returns true/false
 *   - The hash includes the salt — no separate salt column needed
 *   - Work factor 12 is a good default: slow enough to resist brute force,
 *     fast enough that a real user doesn't notice at login
 */
public class PasswordUtil {

    private static final int WORK_FACTOR = 12;

    /*
     * Hash a plain-text password before storing it.
     * Call this in EmployeeServlet (create/update) and AdminServlet.
     */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    /*
     * Verify a plain-text password against a stored BCrypt hash.
     * Call this in AuthServlet instead of .equals().
     */
    public static boolean verify(String plainPassword, String storedHash) {
        return BCrypt.checkpw(plainPassword, storedHash);
    }
}
