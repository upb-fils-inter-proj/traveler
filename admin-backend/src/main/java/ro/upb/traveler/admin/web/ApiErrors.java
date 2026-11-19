package ro.upb.traveler.admin.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Errors answered as ProblemDetail (application/problem+json). */
public final class ApiErrors {

  private ApiErrors() {}

  public static ErrorResponseException of(HttpStatus status, String detail) {
    return new ErrorResponseException(
        status, ProblemDetail.forStatusAndDetail(status, detail), null);
  }

  public static ErrorResponseException notFound(String detail) {
    return of(HttpStatus.NOT_FOUND, detail);
  }

  public static ErrorResponseException conflict(String detail) {
    return of(HttpStatus.CONFLICT, detail);
  }
}
