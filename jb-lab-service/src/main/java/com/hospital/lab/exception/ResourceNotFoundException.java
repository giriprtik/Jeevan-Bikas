package com.hospital.lab.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom Runtime Exception utilized across clinical modules when a requested entity 
 * (such as a Patient MRN, Visit ID, or Lab Order Record) does not exist in the database.
 * 
 * Annotated with @ResponseStatus to guarantee Spring MVC translates this directly into a 404 response.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}