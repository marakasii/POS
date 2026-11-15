package studying.pos.domain.exception;

/** Signals a rejected operation that would violate a domain rule. */
public final class DomainException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a user-readable domain error.
     * @param message explanation of the rejected operation
     */
    public DomainException(final String message) {
        super(message);
    }
}
