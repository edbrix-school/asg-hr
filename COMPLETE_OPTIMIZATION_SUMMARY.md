## Allowance & Deduction Master - Complete Optimization Summary

### Overview
Fully optimized implementation following the Vessel Type Master pattern with 5 core CRUD operations, advanced search, logging, and audit trail.

---

## 1. Entity Layer (HrAllowanceDeductionMaster.java)

✅ **Extends BaseEntity** - Inherits:
- `createdBy` - User who created the record
- `createdDate` - Timestamp of creation
- `lastModifiedBy` - User who last modified
- `lastModifiedDate` - Timestamp of last modification

✅ **Immutable Code Field** - `updatable = false` prevents code changes after creation

✅ **PrePersist Lifecycle** - Auto-sets defaults:
```java
deleted = "N"
active = "Y"
```

---

## 2. DTOs (Request/Response)

### AllowanceDeductionCreateDTO
- Used for **POST** operations
- Includes all fields needed for creation
- Validation annotations for input validation
- Code field is included (immutable after creation)

### AllowanceDeductionUpdateDTO
- Used for **PUT** operations
- Excludes code field (immutable)
- Includes all updatable fields
- Validation annotations

### AllowanceDeductionMasterResponseDTO
- Used for all responses
- Includes audit fields:
  - `createdBy`, `createdDate`
  - `lastModifiedBy`, `lastModifiedDate`
- Uses `LocalDateTime` for date fields (not String)

---

## 3. Service Layer (AllowanceDeductionMasterServiceImpl.java)

### 5 Core CRUD Operations:

#### 1. **CREATE** - `create(AllowanceDeductionCreateDTO)`
```
✓ Validates request
✓ Checks for duplicate code
✓ Checks for duplicate payroll field name
✓ Sets createdBy and createdDate
✓ Logs creation with LogDetailsEnum.CREATED
✓ Returns response with audit trail
```

#### 2. **UPDATE** - `update(Long id, AllowanceDeductionUpdateDTO)`
```
✓ Validates request
✓ Checks if record exists
✓ Prevents update of deleted records
✓ Captures old values for audit trail
✓ Sets lastModifiedBy and lastModifiedDate
✓ Logs changes with LogDetailsEnum.MODIFIED
✓ Uses logChanges() for detailed change tracking
```

#### 3. **GET BY ID** - `getById(Long id)`
```
✓ Retrieves active records only
✓ Throws exception for deleted records
✓ Logs retrieval with LogDetailsEnum.VIEWED
✓ Returns complete response with audit trail
```

#### 4. **SEARCH** - `search(String docId, FilterRequestDto, Pageable)`
```
✓ Uses DocumentSearchService for advanced filtering
✓ Supports dynamic filtering with operators (AND/OR)
✓ Supports pagination and sorting
✓ Returns paginated results with display fields
✓ Logs search operations
```

#### 5. **DELETE** - `softDelete(Long id)`
```
✓ Soft delete (sets deleted = "Y", active = "N")
✓ Prevents deletion of already deleted records
✓ Sets lastModifiedBy and lastModifiedDate
✓ Creates log summary entry with LogDetailsEnum.DELETED
✓ Creates detailed log entries for field changes:
  - Deleted: N → Y
  - Active: Y → N
✓ Throws CustomException if already deleted
```

### Additional Methods:

- `getByCode(String code)` - Get by unique code
- `getAll()` - Get all active records
- `getByGroupPoid(Long groupPoid)` - Filter by group
- `getByType(String type)` - Filter by type (ALLOWANCE/DEDUCTION)
- `getByVariableFixed(String variableFixed)` - Filter by variable/fixed
- `listWithFilters(Map filters, int page, int size)` - Paginated list

---

## 4. Controller Layer (AllowanceDeductionMasterController.java)

### API Endpoints:

```
POST   /v1/allowance-deduction-master/search              - Search with filters
POST   /v1/allowance-deduction-master                     - Create
PUT    /v1/allowance-deduction-master/{id}                - Update
GET    /v1/allowance-deduction-master/{id}                - Get by ID
GET    /v1/allowance-deduction-master/code/{code}         - Get by Code
GET    /v1/allowance-deduction-master                     - Get All
GET    /v1/allowance-deduction-master/group/{groupPoid}   - Get by Group
GET    /v1/allowance-deduction-master/type/{type}         - Get by Type
GET    /v1/allowance-deduction-master/variable-fixed/{vf} - Get by Variable/Fixed
DELETE /v1/allowance-deduction-master/{id}                - Delete (Soft)
POST   /v1/allowance-deduction-master/list                - List with Filters
```

### Search Endpoint Features:

```java
POST /v1/allowance-deduction-master/search
Query Parameters:
  - page: 0 (default)
  - size: 20 (default)
  - sort: "description,asc" (optional)

Request Body (FilterRequestDto):
  - Supports dynamic filtering
  - Supports AND/OR operators
  - Supports field-based search
```

### Delete Endpoint Features:

```java
DELETE /v1/allowance-deduction-master/{id}

Response:
  - 200: Successfully deleted
  - 404: Record not found
  - 400: Cannot delete (already deleted)

Logging:
  - Creates log summary entry
  - Creates detailed log entries for field changes
  - Tracks who deleted and when
```

### List Endpoint Features:

```java
POST /v1/allowance-deduction-master/list
Query Parameters:
  - page: 0 (default)
  - size: 10 (default)

Request Body (optional):
  - filters: Map<String, Object>

Response:
  {
    "status": "success",
    "message": "...",
    "data": {
      "content": [...],
      "totalElements": 100,
      "totalPages": 10,
      "currentPage": 0,
      "pageSize": 10
    }
  }
```

### Error Handling:

- Consistent HTTP status codes
- Proper exception handling
- Detailed error messages
- Logging of all errors

---

## 5. Mapper (AllowanceDeductionMasterMapper.java)

```java
toEntity(AllowanceDeductionCreateDTO)      - DTO → Entity for create
updateEntity(AllowanceDeductionUpdateDTO)  - DTO → Entity for update
toResponseDTO(HrAllowanceDeductionMaster)  - Entity → DTO for response
```

**Features:**
- Proper null handling
- Default value assignment
- Date field mapping (LocalDateTime)
- Audit field mapping

---

## 6. Repository (AllowanceDeductionMasterRepository.java)

**Custom Query Methods:**
```java
findByCodeAndDeletedNot(String code, String deleted)
findByCodeAndGroupPoidAndDeletedNot(String code, Long groupPoid, String deleted)
findByGroupPoidAndDeletedNot(Long groupPoid, String deleted)
findByActiveAndDeletedNot(String active, String deleted)
findByTypeAndDeletedNot(String type, String deleted)
findByVariableFixedAndDeletedNot(String variableFixed, String deleted)
findAllActiveOrderBySeqNo()
findByGroupPoidOrderBySeqNo(Long groupPoid)
findByPayrollFieldName(String payrollFieldName)
findByGlPoid(Long glPoid)
```

---

## 7. Logging Integration

### Log Levels:

| Operation | Log Type | Details |
|-----------|----------|---------|
| CREATE | CREATED | Record ID, timestamp |
| UPDATE | MODIFIED | Old values, new values, field changes |
| DELETE | DELETED | Deleted flag change, Active flag change |
| VIEW | VIEWED | Record ID, timestamp |

### Log Details Entry:

```java
loggingService.createLogDetailsEntry(
    documentId,
    recordId,
    fieldName,
    oldValue,
    newValue,
    logDetail,
    tableName
);
```

---

## 8. Validation

### Create Request Validation:
- Code: Mandatory, alphanumeric, max 20 chars
- Description: Mandatory, max 100 chars
- Type: Mandatory, must be ALLOWANCE or DEDUCTION
- Variable Fixed: Mandatory, max 20 chars
- GL: Mandatory
- Sequence: Optional, must be positive

### Update Request Validation:
- Same as create (except code is excluded)

---

## 9. Key Features

✅ **Audit Trail** - Automatic tracking of creation and modification
✅ **Soft Delete** - Records marked as deleted, not permanently removed
✅ **Immutable Code** - Code cannot be changed after creation
✅ **Advanced Search** - DocumentSearchService integration
✅ **Pagination** - All list endpoints support pagination
✅ **Sorting** - Configurable sort fields and directions
✅ **Logging** - Comprehensive logging at all operations
✅ **Validation** - Input validation with detailed error messages
✅ **Error Handling** - Consistent exception handling
✅ **Security** - Proper authorization checks

---

## 10. Database Schema

```sql
CREATE TABLE HR_ALLOWANCE_DEDUCTION_MASTER (
    ALLOWACE_DEDUCTION_POID BIGINT PRIMARY KEY AUTO_INCREMENT,
    GROUP_POID BIGINT,
    CODE VARCHAR(20) UNIQUE NOT NULL,
    DESCRIPTION VARCHAR(100) NOT NULL,
    VARIABLE_FIXED VARCHAR(20) NOT NULL,
    TYPE VARCHAR(20) NOT NULL,
    FORMULA VARCHAR(500),
    GLCODE VARCHAR(20),
    MANDATORY VARCHAR(1),
    ACTIVE VARCHAR(1),
    SEQNO INT,
    DELETED VARCHAR(1),
    GL_POID BIGINT NOT NULL,
    PAYROLL_FIELD_NAME VARCHAR(30) UNIQUE,
    CREATED_BY VARCHAR(100),
    CREATED_DATE TIMESTAMP,
    LASTMODIFIED_BY VARCHAR(100),
    LASTMODIFIED_DATE TIMESTAMP
);
```

---

## 11. Usage Examples

### Create:
```bash
POST /v1/allowance-deduction-master
{
  "code": "HRA",
  "description": "House Rent Allowance",
  "type": "ALLOWANCE",
  "variableFixed": "FIXED",
  "glPoid": 123,
  "mandatory": "Y",
  "seqno": 1
}
```

### Update:
```bash
PUT /v1/allowance-deduction-master/1
{
  "description": "House Rent Allowance Updated",
  "type": "ALLOWANCE",
  "variableFixed": "FIXED",
  "glPoid": 123,
  "mandatory": "Y",
  "seqno": 1
}
```

### Search:
```bash
POST /v1/allowance-deduction-master/search?page=0&size=20&sort=description,asc
{
  "filters": [
    {
      "fieldName": "type",
      "operator": "equals",
      "value": "ALLOWANCE"
    }
  ],
  "operator": "AND"
}
```

### Delete:
```bash
DELETE /v1/allowance-deduction-master/1
```

---

## Summary

This implementation provides a **production-ready, fully optimized** allowance and deduction master module with:
- Complete CRUD operations
- Advanced search and filtering
- Comprehensive logging and audit trail
- Proper validation and error handling
- Soft delete with dependency checking
- Immutable code field
- Automatic timestamp tracking
- Pagination and sorting support
