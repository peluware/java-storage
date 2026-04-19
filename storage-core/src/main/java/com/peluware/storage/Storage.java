package com.peluware.storage;

import com.peluware.storage.exceptions.AlreadyFileExistsStorageException;
import com.peluware.storage.exceptions.StorageNotFoundException;
import com.peluware.storage.exceptions.InvalidFileNameStorageException;
import com.peluware.storage.exceptions.InvalidPathStorageException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.*;

/**
 * Clase abstracta que representa un almacen de objetos, archivos o binarios
 */
@Getter
@Slf4j
public abstract class Storage implements AutoCloseable {

    @Override
    public void close() throws Exception {
    }

    /**
     * @param storageObject Objeto que contiene la información del archivo a almacenar
     * @throws IOException Si ocurre un error de lectura o escritura al almacenar el archivo
     */
    protected abstract void internalStore(final StorageObject storageObject) throws IOException;

    /**
     * Retorna un {@link Stored} con la metadata del archivo y un loader diferido para su contenido.
     * Si {@link StorageRequest#getRange()} está presente, el loader aplica el rango al abrir el stream.
     *
     * @param request Referencia al archivo con rango opcional
     * @return Stored con metadata y carga diferida, o vacío si no existe
     */
    protected abstract Optional<Stored> internalGet(final StorageRequest request);

    /**
     * Verifica si un archivo existe en el almacenamiento sin recuperar su contenido.
     *
     * <p>
     * Este método se utiliza internamente para validar la existencia de un archivo,
     * por ejemplo antes de realizar operaciones como almacenamiento o eliminación.
     * No debe realizar operaciones costosas como la descarga del contenido.
     * </p>
     *
     * @param ref Referencia al archivo a verificar
     * @return {@code true} si el archivo existe en el almacenamiento, {@code false} en caso contrario
     */
    protected abstract boolean internalExists(final StorageObjectRef ref);

    /**
     * Elimina un archivo del almacen.
     *
     * @param ref Referencia al archivo a eliminar
     * @throws IOException Si ocurre un error de I/O al eliminar el archivo
     */
    protected abstract void internalRemove(final StorageObjectRef ref) throws IOException;

    /**
     * Lista los archivos en un directorio sin descargar su contenido.
     * El contenido se descarga únicamente al invocar {@link Stored#openContent()}.
     *
     * @param directory Directorio a listar
     * @return Lista de entradas con metadata y carga diferida del contenido
     * @throws IOException Si ocurre un error de I/O al listar los archivos
     */
    protected abstract List<Stored> internalList(String directory) throws IOException;


    /**
     * Guarda un archivo en el almacen a partir de un input stream en la ruta raíz
     *
     * @param stream   Flujo del archivo
     * @param filename Nombre del archivo
     * @throws IOException                     Si ocurre un error de lectura o escritura al almacenar el archivo
     * @throws InvalidFileNameStorageException Si el nombre del archivo es inválido
     * @throws InvalidPathStorageException     Si el path es inválido
     */
    public String store(InputStream stream, String filename) throws IOException {
        return store(stream, filename, "");
    }

    /**
     * Guarda un archivo en el almacen a partir de sus bytes en la ruta raíz
     *
     * @param content  Contenido del archivo
     * @param filename Nombre del archivo
     * @throws IOException                     Si ocurre un error de lectura o escritura al almacenar el archivo
     * @throws InvalidFileNameStorageException Si el nombre del archivo es inválido
     * @throws InvalidPathStorageException     Si el path es inválido
     */
    public String store(byte[] content, String filename) throws IOException {
        return store(content, filename, "");
    }

    /**
     * Guarda un archivo en el almacen a partir de un input stream en un path específico
     *
     * @param stream    Flujo del archivo
     * @param filename  Nombre del archivo
     * @param directory Directory donde se almacenará el archivo
     * @throws IOException                     Si ocurre un error de lectura o escritura al almacenar el archivo
     * @throws InvalidFileNameStorageException Si el nombre del archivo es inválido
     * @throws InvalidPathStorageException     Si el path es inválido
     */
    public String store(InputStream stream, String filename, String directory) throws IOException {
        return store(new StorageObject(directory, filename, stream));
    }

    /**
     * Guarda un archivo en el almacen a partir de sus bytes en un path específico
     *
     * @param content   Contenido del archivo
     * @param filename  Nombre del archivo
     * @param directory Directorio donde se almacenará el archivo
     * @return Ruta completa del archivo almacenado
     * @throws IOException                     Si ocurre un error de lectura o escritura al almacenar el archivo
     * @throws InvalidFileNameStorageException Si el nombre del archivo es inválido
     * @throws InvalidPathStorageException     Si el path es inválido
     */
    public String store(byte[] content, String filename, String directory) throws IOException {
        return store(new StorageObject(directory, filename, content));
    }

    /**
     * Guarda un archivo en el almacen a partir de un objeto que contiene la información del archivo a almacenar
     *
     * @param storageObject Objeto que contiene la información del archivo a almacenar
     * @return Ruta completa del archivo almacenado
     * @throws IOException Si ocurre un error de lectura o escritura al almacenar el archivo
     */
    public String store(StorageObject storageObject) throws IOException {
        log.debug("Storing file {} in dir {}", storageObject.getFileName(), storageObject.getDirectory());
        throwIfAlreadyFileExists(storageObject);
        internalStore(storageObject);
        return storageObject.getPath();
    }


    /**
     * Guarda varios archivos en el almacen de forma segura
     *
     * @param storageObjects Objetos que contienen la información de los archivos a almacenar
     * @throws IOException              Si ocurre un error de lectura o escritura al almacenar los archivos
     * @throws StorageNotFoundException Si no se encuentra alguno de los archivos a almacenar
     */
    public void store(StorageObject... storageObjects) throws IOException {
        var storeds = new ArrayList<StorageObject>();
        try {
            Arrays
                .stream(storageObjects)
                .forEach(this::throwIfAlreadyFileExists);
            for (var toStore : storageObjects) {
                internalStore(toStore);
                storeds.add(toStore);
                log.debug("Stored file: {}", toStore.getPath());
            }
        } catch (Exception e) {
            for (var stored : storeds) {
                try {
                    internalRemove(stored);
                } catch (Exception e1) {
                    log.debug("Error purging file: {}", stored.getPath(), e1);
                }
            }
            throw e;
        }
    }

    /**
     * Guarda varios archivos en el almacen de forma segura
     *
     * @param storageObjects Objetos que contienen la información de los archivos a almacenar
     * @throws IOException              Si ocurre un error de lectura o escritura al almacenar los archivos
     * @throws StorageNotFoundException Si no se encuentra alguno de los archivos a almacenar
     */
    public void store(Collection<StorageObject> storageObjects) throws IOException {
        this.store(storageObjects.toArray(StorageObject[]::new));
    }


    private void throwIfAlreadyFileExists(StorageObjectRef ref) {
        if (internalExists(ref)) {
            throw new AlreadyFileExistsStorageException(ref);
        }
    }

    public Optional<Stored> get(String path) {
        var split = SplitPath.from(path);
        return internalGet(new StorageRequest(split.path(), split.filename()));
    }

    public Optional<Stored> get(String path, ByteRange range) {
        var split = SplitPath.from(path);
        return internalGet(new StorageRequest(split.path(), split.filename(), range));
    }

    public Optional<Stored> get(String filename, String directory) {
        return internalGet(new StorageRequest(directory, filename));
    }

    public Optional<Stored> get(String filename, String directory, ByteRange range) {
        return internalGet(new StorageRequest(directory, filename, range));
    }

    public Optional<Stored> info(String path) {
        return get(path);
    }

    public Optional<Stored> info(String filename, String directory) {
        return get(filename, directory);
    }

    public boolean exists(String fullPath) {
        var split = SplitPath.from(fullPath);
        return exists(split.filename(), split.path());
    }

    public boolean exists(String filename, String path) {
        return internalExists(new StorageRequest(path, filename));
    }

    /**
     * Elimina un archivo almacenado a partir de su ruta completa
     *
     * @param fullPath Ruta completa del archivo
     * @throws IOException Si ocurre un error de lectura o escritura al eliminar el archivo
     */
    public void remove(String fullPath) throws IOException {
        var split = SplitPath.from(fullPath);
        remove(split.filename(), split.path());
    }

    /**
     * Elimina un archivo almacenado a partir de su nombre y ruta
     *
     * @param filename Nombre del archivo
     * @param path     Ruta donde se encuentra el archivo
     * @throws IOException Si ocurre un error de lectura o escritura al eliminar el archivo
     */
    public void remove(String filename, String path) throws IOException {
        internalRemove(new StorageRequest(path, filename));
    }


    /**
     * Lista los archivos en un directorio sin descargar su contenido.
     *
     * @param directory Directorio a listar
     * @return Lista de entradas con metadata y carga diferida del contenido
     * @throws IOException Si ocurre un error de I/O al listar los archivos
     */
    public List<Stored> list(String directory) throws IOException {
        StorageAssertions.validDirectory(directory);
        return internalList(StorageUtils.normalizeDirectory(directory));
    }

    /**
     * Lista los archivos en el directorio raíz sin descargar su contenido.
     *
     * @return Lista de entradas con metadata y carga diferida del contenido
     * @throws IOException Si ocurre un error de I/O al listar los archivos
     */
    public List<Stored> list() throws IOException {
        return internalList("");
    }


    /**
     * Genera una URL de descarga de acceso limitado para un archivo almacenado.
     * <p>
     * Si {@link StorageRequest#getRange()} está presente, la URL firmada apuntará
     * únicamente al fragmento de bytes indicado (soportado nativamente en S3).
     * </p>
     *
     * @param request  Referencia al archivo con rango opcional
     * @param duration Duración por la cual la URL será válida
     * @return Una URL de acceso limitado al archivo
     */
    protected abstract URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration);

    /**
     * Genera una URL de carga de acceso limitado para un archivo almacenado.
     *
     * @param ref      Referencia al archivo destino de la carga con metadatos opcionales
     * @param duration Duración por la cual la URL será válida
     * @return Una URL de carga de acceso limitado
     */
    protected abstract URL internalGenerateUploadSignedUrl(StorageUploadRef ref, Duration duration);

    /**
     * Genera una URL de descarga de acceso limitado para un archivo almacenado a partir de su ruta completa.
     *
     * @param fullPath Ruta completa del archivo en el storage
     * @param duration Duración por la cual la URL será válida
     * @return Una URL de acceso limitado al archivo
     */
    public URL generateDownloadSignedUrl(String fullPath, Duration duration) {
        var split = SplitPath.from(fullPath);
        return internalGenerateDownloadSignedUrl(new StorageRequest(split.path(), split.filename()), duration);
    }

    /**
     * Genera una URL de descarga de acceso limitado que apunta únicamente al fragmento indicado por {@code range}.
     *
     * @param fullPath Ruta completa del archivo en el storage
     * @param duration Duración por la cual la URL será válida
     * @param range    Rango de bytes que devolverá la URL firmada
     * @return Una URL de acceso limitado al fragmento del archivo
     */
    public URL generateDownloadSignedUrl(String fullPath, Duration duration, ByteRange range) {
        var split = SplitPath.from(fullPath);
        return internalGenerateDownloadSignedUrl(new StorageRequest(split.path(), split.filename(), range), duration);
    }

    /**
     * Genera una URL de descarga de acceso limitado a partir de un {@link StorageRequest}, permitiendo
     * especificar ruta, nombre y rango de bytes en un único objeto.
     *
     * @param request  Referencia al archivo con rango opcional
     * @param duration Duración por la cual la URL será válida
     * @return Una URL de acceso limitado al archivo (o fragmento)
     */
    public URL generateDownloadSignedUrl(StorageRequest request, Duration duration) {
        return internalGenerateDownloadSignedUrl(request, duration);
    }

    /**
     * Genera una URL de carga de acceso limitado a partir de la ruta completa del archivo destino.
     *
     * @param fullPath Ruta completa del archivo destino en el storage
     * @param duration Duración por la cual la URL será válida
     * @return Una URL de carga de acceso limitado
     */
    public URL generateUploadSignedUrl(String fullPath, Duration duration) {
        var split = SplitPath.from(fullPath);
        return internalGenerateUploadSignedUrl(new StorageUploadRef(split.path(), split.filename()), duration);
    }

    /**
     * Genera una URL de carga de acceso limitado a partir del nombre y directorio del archivo destino.
     *
     * @param filename  Nombre del archivo destino
     * @param directory Directorio donde se almacenará el archivo
     * @param duration  Duración por la cual la URL será válida
     * @return Una URL de carga de acceso limitado
     */
    public URL generateUploadSignedUrl(String filename, String directory, Duration duration) {
        return internalGenerateUploadSignedUrl(new StorageUploadRef(directory, filename), duration);
    }

    /**
     * Genera una URL de carga de acceso limitado a partir de un {@link StorageObjectRef}.
     *
     * @param ref      Referencia al archivo destino de la carga
     * @param duration Duración por la cual la URL será válida
     * @return Una URL de carga de acceso limitado
     */
    public URL generateUploadSignedUrl(StorageObjectRef ref, Duration duration) {
        return internalGenerateUploadSignedUrl(StorageUploadRef.from(ref), duration);
    }

    /**
     * Genera una URL de carga de acceso limitado a partir de un {@link StorageUploadRef},
     * permitiendo especificar metadatos como {@code contentType} y {@code contentLength}
     * que los backends compatibles incluirán en la firma.
     *
     * @param ref      Referencia al archivo destino con metadatos opcionales
     * @param duration Duración por la cual la URL será válida
     * @return Una URL de carga de acceso limitado
     */
    public URL generateUploadSignedUrl(StorageUploadRef ref, Duration duration) {
        return internalGenerateUploadSignedUrl(ref, duration);
    }


    /**
     * Elimina los archivos almacenados a partir de un objeto que contiene las referencias a los archivos
     * a eliminar
     *
     * @param purgable Objeto que contiene las referencias a los archivos a eliminar
     * @throws IOException              Si ocurre un error de lectura o escritura al eliminar los archivos almacenados a partir del objeto purgable
     * @throws StorageNotFoundException Si no se encuentra el archivo a eliminar
     */
    public void purge(PurgableStored purgable) throws IOException {
        for (var fullPath : purgable.filesFullPaths()) {
            remove(fullPath);
        }
    }

    /**
     * Elimina los archivos almacenados a partir de una colección de objetos que contienen las referencias a los archivos
     *
     * @param purgables Objetos que contienen las referencias a los archivos a eliminar
     * @throws IOException              Si ocurre un error de lectura o escritura al eliminar los archivos almacenados a partir de los objetos purgables
     * @throws StorageNotFoundException Si no se encuentra alguno de los archivos a eliminar
     */
    public void purge(Iterable<? extends PurgableStored> purgables) throws IOException {
        for (var purgable : purgables) {
            purge(purgable);
        }
    }

    /**
     * Transfiere un archivo almacenado a otro almacen
     *
     * @param target   Almacen donde se almacenará el archivo
     * @param filename Nombre del archivo
     * @param path     Ruta donde se encuentra el archivo
     * @throws IOException Si ocurre un error de lectura o escritura al transferir el archivo
     */
    public void transferTo(Storage target, String filename, String path) throws IOException {
        var pathFile = new StorageRequest(path, filename);
        var file = internalGet(pathFile);
        if (file.isEmpty()) {
            throw new StorageNotFoundException(pathFile);
        }
        target.store(file.get().openContent(), filename, path);
    }

    /**
     * Transfiere un archivo almacenado a otro almacen
     *
     * @param target   Almacen donde se almacenará el archivo
     * @param fullPath Ruta completa del archivo
     * @throws IOException Si ocurre un error de lectura o escritura al transferir el archivo
     */
    public void transferTo(Storage target, String fullPath) throws IOException {
        var split = SplitPath.from(fullPath);
        transferTo(target, split.filename(), split.path());
    }

    record SplitPath(String path, String filename) {

        public static SplitPath from(String path) {
            var split = new SplitPath(extractDirectory(path), extractFilename(path));
            log.debug("Split path: {}", split);
            return split;
        }

        /**
         * Extrae la ruta de un archivo a partir de su ruta completa
         *
         * @param path Ruta completa del archivo
         * @return Ruta del archivo
         */
        private static String extractDirectory(String path) {
            return path.contains("/") ? path.substring(0, path.lastIndexOf("/") + 1) : "/";
        }

        /**
         * Extrae el nombre de un archivo a partir de su ruta completa
         *
         * @param path Ruta completa del archivo
         * @return Nombre del archivo
         */
        private static String extractFilename(String path) {
            return path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
        }

        @Override
        public String toString() {
            return "[path= " + path + ", filename= " + filename + "]";
        }
    }
}

