package com.asg.hr.exceptions;

import com.asg.hr.common.dto.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestURI()).thenReturn("/test/uri");
    }

    @Test
    void testHandleCustomException() {
        CustomException ex = new CustomException("Custom error", 400);
        ResponseEntity<?> response = handler.handleAsgException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleValidationException() {
        jakarta.xml.bind.ValidationException ex = new jakarta.xml.bind.ValidationException("Validation error");
        ResponseEntity<?> response = handler.handleValidationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Illegal argument");
        ResponseEntity<?> response = handler.handleIllegalArgumentException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleDateFormatException() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getRequiredType()).thenReturn((Class) LocalDate.class);
        when(ex.getValue()).thenReturn("invalid-date");
        when(ex.getPropertyName()).thenReturn("startDate");
        
        ResponseEntity<?> response = handler.handleDateFormatException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field1", "Field error");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError));
        
        ResponseEntity<?> response = handler.handleValidationExceptions(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleMissingPathVariable() {
        MissingPathVariableException ex = new MissingPathVariableException("id", null);
        ResponseEntity<?> response = handler.handleMissingPathVariable(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleMissingHeader() {
        MissingRequestHeaderException ex = new MissingRequestHeaderException("Authorization", null);
        ResponseEntity<?> response = handler.handleMissingHeader(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleResourceAlreadyExists() {
        ResourceAlreadyExistsException ex = new ResourceAlreadyExistsException("Resource exists");
        ResponseEntity<?> response = handler.handleResourceAlreadyExists(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");
        ResponseEntity<?> response = handler.handleResourceNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testHandleNoResourceFoundException() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("No resource found");
        ResponseEntity<?> response = handler.handleNoResourceFoundException(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testHandleHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(new IllegalArgumentException("Invalid JSON"));
        ResponseEntity<?> response = handler.handleJsonParseErrors(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleMaxUploadSizeExceededException() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1000);
        ResponseEntity<?> response = handler.handleMaxUploadSize(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleJpaSystemException() {
        JpaSystemException ex = mock(JpaSystemException.class);
        when(ex.getMostSpecificCause()).thenReturn(new RuntimeException("DB error\nDetails"));
        ResponseEntity<?> response = handler.handleJpaSystemException(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHandleRuntimeException() {
        RuntimeException ex = new RuntimeException("Runtime error");
        ResponseEntity<?> response = handler.handleRuntimeException(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHandleGeneralException() {
        Exception ex = new Exception("General error");
        ResponseEntity<?> response = handler.handleGeneralException(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHandleCommonLibResourceAlreadyExists() {
        com.asg.common.lib.exception.ResourceAlreadyExistsException ex = 
            new com.asg.common.lib.exception.ResourceAlreadyExistsException("Department", "IT");
        ResponseEntity<?> response = handler.handleResourceAlreadyExists(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testHandleCommonLibResourceNotFound() {
        com.asg.common.lib.exception.ResourceNotFoundException ex = 
            new com.asg.common.lib.exception.ResourceNotFoundException("Department", "id", 1L);
        ResponseEntity<?> response = handler.handleResourceNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testHandleShippingValidationException() {
        List<ValidationError> errors = Arrays.asList(new ValidationError(null, "field1", "Error"));
        ValidationException ex = new ValidationException("Validation failed", errors);
        ResponseEntity<?> response = handler.handleShippingValidationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleShippingValidationExceptionWithoutFieldErrors() {
        ValidationException ex = new ValidationException("Validation failed");
        ResponseEntity<?> response = handler.handleShippingValidationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleCommonLibValidationException() {
        com.asg.common.lib.exception.ValidationException ex = 
            new com.asg.common.lib.exception.ValidationException("Validation failed");
        ResponseEntity<?> response = handler.handleValidationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
