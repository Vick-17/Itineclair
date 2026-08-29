package fr.itineclair.track;

public class GpxFileTooLargeException extends RuntimeException {

    public GpxFileTooLargeException() {
        super("Le fichier GPX dépasse la taille maximale autorisée de 10 Mo.");
    }
}
