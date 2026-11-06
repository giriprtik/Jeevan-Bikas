package com.hospital.opd.exception;

import org.springframework.http.HttpStatus;

public class DownstreamServiceException extends RuntimeException {
    private final String service;
    private final Integer downstreamStatus;   // null = no response (unreachable / timeout)
    private final HttpStatus clientStatus;

    public DownstreamServiceException(String service, Integer downstreamStatus,
                                      HttpStatus clientStatus, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
        this.downstreamStatus = downstreamStatus;
        this.clientStatus = clientStatus;
    }
    public String getService() { return service; }
    public Integer getDownstreamStatus() { return downstreamStatus; }
    public HttpStatus getClientStatus() { return clientStatus; }
}