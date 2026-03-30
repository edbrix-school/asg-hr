package com.asg.hr.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceAlreadyExistsExceptionTest {

    @Test
    void testConstructorWithMessage() {
        ResourceAlreadyExistsException ex = new ResourceAlreadyExistsException("Resource exists");
        assertEquals("Resource exists", ex.getMessage());
        assertNull(ex.getFieldName());
        assertNull(ex.getFieldValue());
    }

    @Test
    void testConstructorWithFieldNameAndValue() {
        ResourceAlreadyExistsException ex = new ResourceAlreadyExistsException("Department", "IT");
        assertEquals("Department already exists with value: IT", ex.getMessage());
        assertEquals("Department", ex.getFieldName());
        assertEquals("IT", ex.getFieldValue());
    }
}
