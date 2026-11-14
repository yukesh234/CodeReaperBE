package com.codereaper.dtos.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
@AllArgsConstructor
public class ErrorResponse {
    public String message;
    public HttpStatus status;
}
