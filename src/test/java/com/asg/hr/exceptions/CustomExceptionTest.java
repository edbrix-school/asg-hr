package com.asg.hr.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    void testConstructorWithMessage() {
        CustomException ex = new CustomException("Test error");
        assertEquals("Test error", ex.getMessage());
        assertEquals(500, ex.getCode());
    }

    @Test
    void testConstructorWithMessageAndCode() {
        CustomException ex = new CustomException("Test error", 400);
        assertEquals("Test error", ex.getMessage());
        assertEquals(400, ex.getCode());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        CustomException ex = new CustomException("Test error", cause);
        assertEquals("Test error", ex.getMessage());
        assertEquals(500, ex.getCode());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testConstructorWithMessageCodeAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        CustomException ex = new CustomException("Test error", 404, cause);
        assertEquals("Test error", ex.getMessage());
        assertEquals(404, ex.getCode());
        assertEquals(cause, ex.getCause());
    }
}
