package app.assignment;

/**
 * No Assignment matches the requested id for the calling Educator — either it never existed or
 * it belongs to someone else. Both cases return the same {@code 404}, never a {@code 403}, so
 * that another Educator's Assignment is never disclosed to exist.
 */
public class AssignmentNotFoundException extends RuntimeException {

    public AssignmentNotFoundException(String message) {
        super(message);
    }
}
