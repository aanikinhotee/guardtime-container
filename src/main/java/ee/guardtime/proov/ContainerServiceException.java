package ee.guardtime.proov;

/**
 * Common exception in current project.
 *
 */
public class ContainerServiceException extends RuntimeException {

  public ContainerServiceException(String message) {
    super(message);
  }

  public ContainerServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
