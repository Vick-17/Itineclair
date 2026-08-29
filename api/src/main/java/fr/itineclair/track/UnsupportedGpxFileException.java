package fr.itineclair.track;

public class UnsupportedGpxFileException extends RuntimeException {

    public UnsupportedGpxFileException() {
        super("Le fichier doit utiliser l’extension .gpx.");
    }
}
