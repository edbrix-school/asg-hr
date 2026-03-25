package com.asg.hr.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void testConstructorWithMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");
        assertEquals("Resource not found", ex.getMessage());
        assertNull(ex.getResourceName());
        assertNull(ex.getFieldName());
        assertNull(ex.getFieldValue());
    }

    @Test
    void testConstructorWithResourceFieldAndValue() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Department", "id", 123L);
        assertEquals("Department not found with id : '123'", ex.getMessage());
        assertEquals("Department", ex.getResourceName());
        assertEquals("id", ex.getFieldName());
        assertEquals(123L, ex.getFieldValue());
    }
}
