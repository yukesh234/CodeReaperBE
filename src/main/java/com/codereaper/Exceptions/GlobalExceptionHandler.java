package com.codereaper.Exceptions;

import com.codereaper.dtos.error.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GlobalExceptionHandler {

    @ExceptionHandler({
            Exception.class,
            IllegalArgumentException.class,
            IllegalStateException.class,
            ResourceNotFound.class
    })
    public ResponseEntity<ErrorResponse> handleException(Exception e){
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_GATEWAY);
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(Auth.class)
    public ResponseEntity<ErrorResponse> handleAuth(Auth auth){
        ErrorResponse errorResponse = new ErrorResponse(auth.getMessage(), HttpStatus.UNAUTHORIZED);
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }
}
