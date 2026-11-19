package ro.upb.traveler.admin.storage;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Answered as 503: the photo storage is not configured or cannot be reached. */
public class StorageUnavailableException extends ErrorResponseException {

  public StorageUnavailableException(Throwable cause) {
    super(
        HttpStatus.SERVICE_UNAVAILABLE,
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE, "The photo storage is unavailable."),
        cause);
  }
}
