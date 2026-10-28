package com.zodiakk.projectservice.common.exception.handler;

import com.zodiakk.projectservice.board.exception.board.ProjectBoardAlreadyExistsException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnCodeAlreadyExistsException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnInUseException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnPositionAlreadyExistsException;
import com.zodiakk.projectservice.common.dto.field.ValidationErrorField;
import com.zodiakk.projectservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.projectservice.common.dto.response.ValidationErrorResponse;
import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;
import com.zodiakk.projectservice.integration.exception.EmployeeServiceUnavailableException;
import com.zodiakk.projectservice.project.exception.assignment.ProjectAssignmentAlreadyExistsException;
import com.zodiakk.projectservice.project.exception.project.InvalidProjectStatusTransitionException;
import com.zodiakk.projectservice.project.exception.project.ProjectCodeAlreadyExistsException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleCodeAlreadyExistsException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleInUseException;
import com.zodiakk.projectservice.task.exception.assignee.InvalidTaskAssigneeException;
import com.zodiakk.projectservice.task.exception.assignee.TaskAssigneeAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.relation.CrossProjectRelationException;
import com.zodiakk.projectservice.task.exception.relation.TaskDependencyCycleException;
import com.zodiakk.projectservice.task.exception.relation.TaskRelationAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.skill.TaskSkillAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.task.CrossProjectParentException;
import com.zodiakk.projectservice.task.exception.task.InvalidParentTaskTypeException;
import com.zodiakk.projectservice.task.exception.task.TaskArchivedException;
import com.zodiakk.projectservice.task.exception.task.TaskBlockedException;
import com.zodiakk.projectservice.task.exception.task.TaskHasChildrenException;
import com.zodiakk.projectservice.task.exception.task.TaskHierarchyCycleException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeCodeAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeInUseException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeInactiveException;import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            ProjectCodeAlreadyExistsException.class,
            InvalidProjectStatusTransitionException.class,
            ProjectRoleCodeAlreadyExistsException.class,
            ProjectRoleInUseException.class,
            ProjectAssignmentAlreadyExistsException.class,
            ProjectBoardAlreadyExistsException.class,
            BoardColumnCodeAlreadyExistsException.class,
            BoardColumnPositionAlreadyExistsException.class,
            BoardColumnInUseException.class,
            TaskTypeCodeAlreadyExistsException.class,
            TaskTypeInUseException.class,
            TaskTypeInactiveException.class,
            TaskArchivedException.class,
            TaskHasChildrenException.class,
            InvalidParentTaskTypeException.class,
            CrossProjectParentException.class,
            TaskHierarchyCycleException.class,
            TaskBlockedException.class,
            TaskRelationAlreadyExistsException.class,
            CrossProjectRelationException.class,
            TaskDependencyCycleException.class,
            TaskAssigneeAlreadyExistsException.class,
            InvalidTaskAssigneeException.class,
            TaskSkillAlreadyExistsException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(BusinessException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmployeeServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeServiceUnavailable(EmployeeServiceUnavailableException ex) {
        log.warn("Employee service call failed", ex.getCause());

        return error(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Concurrent modification detected", ex);

        return error(HttpStatus.CONFLICT, ErrorMessages.CONCURRENT_MODIFICATION);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, ErrorMessages.ACCESS_DENIED);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return fieldValidationError(ex.getBindingResult().getFieldErrors());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ValidationErrorResponse> handleBinding(BindException ex) {
        return fieldValidationError(ex.getBindingResult().getFieldErrors());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ValidationErrorField> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> ValidationErrorField.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .build())
                .toList();

        return validationError(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, ErrorMessages.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ErrorMessages.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, ErrorMessages.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorMessages.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(
                HttpStatus.BAD_REQUEST,
                ErrorMessages.INVALID_PARAMETER.formatted(ex.getName())
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return error(
                HttpStatus.BAD_REQUEST,
                ErrorMessages.MISSING_PARAMETER.formatted(ex.getParameterName())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database integrity constraint violation", ex);

        return error(HttpStatus.CONFLICT, ErrorMessages.DATA_INTEGRITY_VIOLATION);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInternal(Exception ex) {
        log.error("Unhandled exception", ex);

        return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<ValidationErrorResponse> fieldValidationError(List<FieldError> fieldErrors) {
        List<ValidationErrorField> errors = fieldErrors.stream()
                .map(this::mapFieldError)
                .toList();

        return validationError(errors);
    }

    private ResponseEntity<ValidationErrorResponse> validationError(List<ValidationErrorField> errors) {
        ValidationErrorResponse response = ValidationErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    private ValidationErrorField mapFieldError(FieldError error) {
        return ValidationErrorField.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .build();
    }
}
