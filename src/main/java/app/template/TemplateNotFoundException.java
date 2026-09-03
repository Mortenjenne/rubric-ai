package app.template;

/** No bundled Template exists with the requested id — a client-facing 404, not a server fault. */
public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(String message) {
        super(message);
    }
}
