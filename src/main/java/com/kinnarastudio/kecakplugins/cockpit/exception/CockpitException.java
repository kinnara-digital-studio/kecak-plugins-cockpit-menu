package com.kinnarastudio.kecakplugins.cockpit.exception;

public class CockpitException extends Exception {
    CockpitException(Throwable cause) {
        super(cause);
    }

    public CockpitException(String message) {
        super(message);
    }
}
