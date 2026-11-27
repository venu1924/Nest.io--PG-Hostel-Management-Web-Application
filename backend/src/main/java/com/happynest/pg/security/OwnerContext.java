package com.happynest.pg.security;

import com.happynest.pg.exception.ApiException;
import org.springframework.http.HttpStatus;

public final class OwnerContext {

    private static final ThreadLocal<String> OWNER = new ThreadLocal<>();

    private OwnerContext() {}

    public static void set(String ownerId) {
        OWNER.set(ownerId);
    }

    public static void clear() {
        OWNER.remove();
    }

    public static String current() {
        return OWNER.get();
    }

    public static String require() {
        String ownerId = OWNER.get();
        if (ownerId == null || ownerId.isBlank()) {
            throw new ApiException("Please log in.", HttpStatus.UNAUTHORIZED);
        }
        return ownerId;
    }
}
