package com.example.tt_backend.exception;
public class CampagneInvalideException extends RuntimeException {
    public CampagneInvalideException() {
        super("Campagne invalide ou inactive");
    }
}
