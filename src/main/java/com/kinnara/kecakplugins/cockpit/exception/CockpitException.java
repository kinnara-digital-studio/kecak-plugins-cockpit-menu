package com.kinnara.kecakplugins.cockpit.exception;

public class CockpitException extends Exception {
    CockpitException(Throwable cause) {
        super(cause);
    }

    public CockpitException(String message) {
        super(message);
    }
}
