package fr.mtpilates.shared.storage;

/** Échec d'entrée/sortie du stockage, non récupérable par l'appelant. */
public class StorageException extends RuntimeException {

    public StorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
