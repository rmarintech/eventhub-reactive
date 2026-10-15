package com.rubenmarin.bookingservice.infrastructure.adapter.in;

import com.rubenmarin.bookingservice.application.exception.*;
import com.rubenmarin.bookingservice.infrastructure.web.generated.model.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {

        String detail = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = new ProblemDetail(
                "about:blank",
                "Bad Request",
                HttpStatus.BAD_REQUEST.value()
        );

        problem.setDetail(detail);
        problem.setCode("VALIDATION_ERROR");
        problem.setTimestamp(OffsetDateTime.now());


        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }


    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleBookingNotFound(BookingNotFoundException ex) {


        ProblemDetail problem = new ProblemDetail(
                "about:blank",
                "Booking Not Found",
                HttpStatus.NOT_FOUND.value()
        );

        problem.setDetail(ex.getMessage());
        problem.setCode("BOOKING_NOT_FOUND");
        problem.setTimestamp(OffsetDateTime.now());


        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(EventNotAvailableException.class)
    public ResponseEntity<ProblemDetail> handleEventNotAvailableException(EventNotAvailableException ex) {


        ProblemDetail problem = new ProblemDetail(
                "about:blank",
                "Event not available",
                HttpStatus.CONFLICT.value()
        );

        problem.setDetail(ex.getMessage());
        problem.setCode("EVENT_NOT_AVAILABLE");
        problem.setTimestamp(OffsetDateTime.now());


        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEventNotFoundException(EventNotFoundException ex) {


        ProblemDetail problem = new ProblemDetail(
                "about:blank",
                "Event not found",
                HttpStatus.NOT_FOUND.value()
        );

        problem.setDetail(ex.getMessage());
        problem.setCode("EVENT_NOT_FOUND");
        problem.setTimestamp(OffsetDateTime.now());


        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(BookingAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleBookingAlreadyExistsException(BookingAlreadyExistsException ex) {


        ProblemDetail problem = new ProblemDetail(
                "about:blank",
                "Conflict",
                HttpStatus.CONFLICT.value()
        );

        problem.setDetail(ex.getMessage());
        problem.setCode("BOOKING_ALREADY_EXISTS");
        problem.setTimestamp(OffsetDateTime.now());


        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(EventServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleEventServiceUnavailableException(EventServiceUnavailableException ex) {


        ProblemDetail problem = new ProblemDetail(
                "about:blank",
                "Event Service not available",
                HttpStatus.SERVICE_UNAVAILABLE.value()
        );

        problem.setDetail(ex.getMessage());
        problem.setCode("EVENT_SERVICE_UNAVAILABLE");
        problem.setTimestamp(OffsetDateTime.now());


        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }


}
