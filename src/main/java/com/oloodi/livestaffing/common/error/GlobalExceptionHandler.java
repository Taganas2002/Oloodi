package com.oloodi.livestaffing.common.error;

import com.oloodi.livestaffing.assignment.service.IllegalTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String OPTIMISTIC_LOCK_MESSAGE =
            "The assignment was changed by another session. Reload the latest state.";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalTransitionException.class)
    public ProblemDetail handleIllegalTransition(
            IllegalTransitionException ex, HttpServletRequest request) {
        log.info(
                "Conflict illegal-transition path={} action={} fromParticipation={} fromAttendance={}",
                request.getRequestURI(),
                ex.getAction(),
                ex.getFromParticipation(),
                ex.getFromAttendance());
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
        log.info("Conflict path={} detail={}", request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({
        ObjectOptimisticLockingFailureException.class,
        OptimisticLockingFailureException.class,
        StaleObjectStateException.class
    })
    public ProblemDetail handleOptimisticLock(Exception ex, HttpServletRequest request) {
        log.info(
                "Conflict optimistic-lock path={} type={}",
                request.getRequestURI(),
                ex.getClass().getSimpleName());
        return problem(HttpStatus.CONFLICT, OPTIMISTIC_LOCK_MESSAGE, request);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        IllegalArgumentException.class
    })
    public ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        String detail = ex instanceof MethodArgumentNotValidException manv
                ? manv.getBindingResult().getFieldErrors().stream()
                        .map(err -> err.getField() + ": " + err.getDefaultMessage())
                        .findFirst()
                        .orElse("Validation failed")
                : ex.getMessage();
        return problem(HttpStatus.BAD_REQUEST, detail != null ? detail : "Bad request", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }
}
