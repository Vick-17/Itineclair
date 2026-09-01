package fr.itineclair.sharing;

public class SharedReportNotFoundException extends RuntimeException {

    public SharedReportNotFoundException() {
        super("Shared report unavailable.");
    }
}
