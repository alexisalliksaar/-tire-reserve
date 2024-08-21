package com.alexisa.tire_reserve.exceptions.service.workshops.response;

import com.alexisa.tire_reserve.model.domain.error.WsServiceErrorDescription;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class WsServiceResponseException extends RuntimeException {
    protected final WsServiceErrorDescription serviceErrorResponse;
    private final HttpStatusCode responseStatusCode;

    public WsServiceResponseException(WsServiceErrorDescription serviceErrorResponse, HttpStatusCode responseStatusCode) {
        this.serviceErrorResponse = serviceErrorResponse;
        this.responseStatusCode = responseStatusCode;
    }

    public WsServiceResponseException(String message, WsServiceErrorDescription serviceErrorResponse, HttpStatusCode responseStatusCode) {
        super(message);
        this.serviceErrorResponse = serviceErrorResponse;
        this.responseStatusCode = responseStatusCode;
    }

}
