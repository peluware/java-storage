package com.peluware.storage;

import com.peluware.storage.exceptions.InvalidFileNameStorageException;
import com.peluware.storage.exceptions.InvalidPathStorageException;
import com.peluware.storage.exceptions.StorageException;

import java.util.regex.Pattern;

public final class StorageAssertions {

    private static final Pattern INVALID_FILENAME_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final int MAX_FILENAME_LENGTH = 255;

    private StorageAssertions() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Lanza una excepción si el nombre del archivo es inválido
     *
     * @param filename Nombre del archivo a validar
     * @throws InvalidFileNameStorageException Si el nombre del archivo es inválido
     */
    public static void validFilename(String filename) throws InvalidFileNameStorageException {

        if (filename.isEmpty()) {
            throw new InvalidFileNameStorageException(filename, "The filename is required.");
        }

        if (filename.length() > MAX_FILENAME_LENGTH) {
            throw new InvalidFileNameStorageException(filename, "The filename is too long, it must be less than " + MAX_FILENAME_LENGTH + " characters.");
        }

        var invalidCharacters = new StringBuilder();
        for (char c : filename.toCharArray()) {
            var charact = String.valueOf(c);
            if (INVALID_FILENAME_CHARACTERS.matcher(charact).find() && invalidCharacters.indexOf(charact) == -1) {
                invalidCharacters.append(c).append(" ");
            }
        }

        if (!invalidCharacters.isEmpty()) {
            throw new InvalidFileNameStorageException(filename, "The filename contains invalid characters: " + invalidCharacters.toString().trim());
        }
    }


    private static final Pattern INVALID_PATH_CHARACTERS = Pattern.compile("[<>:\"|?*\\\\]");

    /**
     * Lanza una excepción si el path es inválido
     *
     * @param path Path a validar
     * @throws StorageException Si el path es inválido
     */
    public static void validDirectory(String path) throws InvalidPathStorageException {
        if (path.isBlank() || path.equals("/")) {
            return;
        }

        var invalidCharacters = new StringBuilder();
        for (char c : path.toCharArray()) {
            var charact = String.valueOf(c);
            if (INVALID_PATH_CHARACTERS.matcher(charact).find() && invalidCharacters.indexOf(charact) == -1) {
                invalidCharacters.append(c).append(" ");
            }
        }

        if (!invalidCharacters.isEmpty()) {
            throw new InvalidPathStorageException(path, "The path contains invalid characters: " + invalidCharacters.toString().trim());
        }

        validateInPathSegments(path);
    }

    private static void validateInPathSegments(String path) {
        var segments = path.startsWith("/") ? path.substring(1).split("/") : path.split("/");

        for (var segment : segments) {

            if (segment.isEmpty()) {
                throw new InvalidPathStorageException(path, "The path cannot contain empty segments.");
            }

            if (segment.startsWith(" ")) {
                throw new InvalidPathStorageException(path, "The path cannot contain segments starting with spaces.");
            }

            if (segment.endsWith(" ")) {
                throw new InvalidPathStorageException(path, "The path cannot contain segments ending with spaces.");
            }

            if (segment.endsWith(".")) {
                throw new InvalidPathStorageException(path, "The path cannot contain segments ending with a dot.");
            }

            if (segment.chars().allMatch(c -> c == '.')) {
                throw new InvalidPathStorageException(path, "The path cannot contain segments with only dots.");
            }
        }
    }
}
