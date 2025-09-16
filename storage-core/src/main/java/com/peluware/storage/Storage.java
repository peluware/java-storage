package com.peluware.storage;

import com.peluware.storage.exceptions.AlreadyFileExistsStorageException;
import com.peluware.storage.exceptions.FileNotFoundStorageException;
import com.peluware.storage.exceptions.InvalidFileNameStorageException;
import com.peluware.storage.exceptions.InvalidPathStorageException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Clase abstracta que representa un almacen de archivos
 */
@Getter
@Slf4j
public abstract class Storage {

    private final StoreTracking traking = new StoreTracking();

    private static final ThreadLocal<StorageBatchState> CONTEXT_BATCH_STATE = new ThreadLocal<>();

    @FunctionalInterface
    public interface StorageBatchOperation {
        void excute(StorageBatchState state) throws IOException;
    }

    public static StorageBatchState getContextBatchState() {
        var executionContext = CONTEXT_BATCH_STATE.get();
        if (executionContext == null) {
            throw new IllegalStateException("No execution context found");
        }
        return executionContext;
    }

    public void batchExecution(StorageBatchOperation context) throws IOException {
        var state = new StorageBatchState();
        CONTEXT_BATCH_STATE.set(state);
        try {
            context.excute(state);
            store(state.toStores());
            CompletableFuture.runAsync(() -> {
                for (var fullPath : state.toRemove()) {
                    try {
                        remove(fullPath);
                    } catch (IOException e) {
                        log.error("Error removing file: {}", fullPath, e);
                    }
                }
            });
        } finally {
            CONTEXT_BATCH_STATE.remove();
        }
    }


    /**
     * @param toStore Objeto que contiene la información del archivo a almacenar
     * @throws IOException Si ocurre un error de lectura o escritura al almacenar el archivo
     */
    protected abstract void internalStore(final ToStore toStore) throws IOException;

    /**
     * Descarga un archivo almacenado a partir de su nombre y ruta
     *
     * @param pathFile Objeto que contiene el nombre y ruta del archivo
     * @return Objeto que representa el archivo almacenado
     * @throws IOException Si ocurre un error de lectura o escritura al descargar el archivo
     */
    protected abstract Optional<Stored> internalDownload(final PathFile pathFile) throws IOException;

    /**
     * Obtiene la información de un archivo almacenado a partir de su nombre y ruta
     *
     * @param pathFile Objeto que contiene el nombre y ruta del archivo
     * @return Objeto que representa la información del archivo almacenado
     * @throws IOException Si ocurre un error de lectura o escritura al obtener la información del archivo
     */
    protected abstract Optional<Stored.Info> internalInfo(final PathFile pathFile) throws IOException;

    /**
     * Verifica si un archivo almacenado existe a partir de su nombre y ruta
     *
     * @param pathFile Objeto que contiene el nombre y ruta del archivo
     * @return Si el archivo existe o no
     * @throws IOException Si ocurre un error de lectura o escritura al verificar la existencia del archivo
     */
    protected abstract boolean internalExists(final PathFile pathFile) throws IOException;

    /**
     * Elimina un archivo almacenado a partir de su nombre y ruta
     *
     * @param pathFile Objeto que contiene el nombre y ruta del archivo
     * @throws IOException Si ocurre un error de lectura o escritura al eliminar el archivo
     */
    protected abstract void internalRemove(final PathFile pathFile) throws IOException;


    /**
     * Guarda un archivo en el almacen a partir de un input stream en la ruta raíz
     *
     * @param stream  Flujo del archivo
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
     * @param stream  Flujo del archivo
     * @param filename Nombre del archivo
     * @param path     Ruta donde se almacenará el archivo
     * @throws IOException                     Si ocurre un error de lectura o escritura al almacenar el archivo
     * @throws InvalidFileNameStorageException Si el nombre del archivo es inválido
     * @throws InvalidPathStorageException     Si el path es inválido
     */
    public String store(InputStream stream, String filename, String path) throws IOException {
        return store(new ToStore(path, filename, stream));
    }

    /**
     * Guarda un archivo en el almacen a partir de sus bytes en un path específico
     *
     * @param content  Contenido del archivo
     * @param filename Nombre del archivo
     * @param path     Ruta donde se almacenará el archivo
     * @return Ruta completa del archivo almacenado
     * @throws IOException                     Si ocurre un error de lectura o escritura al almacenar el archivo
     * @throws InvalidFileNameStorageException Si el nombre del archivo es inválido
     * @throws InvalidPathStorageException     Si el path es inválido
     */
    public String store(byte[] content, String filename, String path) throws IOException {
        return store(new ToStore(path, filename, content));
    }


    /**
     * Guarda un archivo en el almacen a partir de un objeto que contiene la información del archivo a almacenar
     * @param toStore Objeto que contiene la información del archivo a almacenar
     * @return Ruta completa del archivo almacenado
     * @throws IOException Si ocurre un error de lectura o escritura al almacenar el archivo
     */
    public String store(ToStore toStore) throws IOException {
        log.debug("Storing file {} in path {}", toStore.getFileName(), toStore.getPath());
        throwIfAlreadyFileExists(toStore);
        internalStore(toStore);
        var completedPath = toStore.getCompletePath();
        traking.track(completedPath);
        return completedPath;
    }


    /**
     * Guarda varios archivos en el almacen de forma segura
     *
     * @param toStores Objetos que contienen la información de los archivos a almacenar
     * @throws IOException Si ocurre un error de lectura o escritura al almacenar los archivos
     * @throws FileNotFoundStorageException Si no se encuentra alguno de los archivos a almacenar
     */
    public void store(ToStore... toStores) throws IOException {
        var storeds = new ArrayList<ToStore>();
        try {
            for (var toStore : toStores) {
                throwIfAlreadyFileExists(toStore);
            }
            for (var toStore : toStores) {
                internalStore(toStore);
                storeds.add(toStore);
                log.debug("Stored file: {}", toStore.getCompletePath());
            }
        } catch (Exception e) {
            for (var stored : storeds) {
                try {
                    internalRemove(stored);
                } catch (Exception e1) {
                    log.debug("Error purging file: {}", stored.getCompletePath(), e1);
                }
            }
            throw e;
        }
        traking.track(storeds.stream().map(PathFile::getCompletePath).toList());
    }

    /**
     * Guarda varios archivos en el almacen de forma segura
     *
     * @param toStores Objetos que contienen la información de los archivos a almacenar
     * @throws IOException Si ocurre un error de lectura o escritura al almacenar los archivos
     * @throws FileNotFoundStorageException Si no se encuentra alguno de los archivos a almacenar
     */
    public void store(Collection<ToStore> toStores) throws IOException {
        this.store(toStores.toArray(ToStore[]::new));
    }


    /**
     * Lanza una excepción si el archivo ya existe
     * @param pathFile Objeto que contiene el nombre y ruta del archivo
     * @throws IOException Si ocurre un error de lectura o escritura al verificar la existencia del archivo
     * @throws AlreadyFileExistsStorageException Si el archivo ya existe
     */
    private void throwIfAlreadyFileExists(PathFile pathFile) throws IOException, AlreadyFileExistsStorageException {
        if (internalExists(pathFile)) {
            throw new AlreadyFileExistsStorageException(pathFile);
        }
    }

    /**
     * Descarga un archivo almacenado a partir de su ruta completa
     *
     * @param fullPath Ruta completa del archivo
     * @return Objeto que representa el archivo almacenado
     * @throws IOException Si ocurre un error de lectura o escritura al descargar el archivo
     */
    public Optional<Stored> download(String fullPath) throws IOException {
        var split = SplitPath.from(fullPath);
        return download(split.filename(), split.path());
    }

    /**
     * @param filename Nombre del archivo
     * @param path     Ruta donde se encuentra el archivo
     * @return Objeto que representa el archivo almacenado
     * @throws IOException Si ocurre un error de lectura o escritura al descargar el archivo
     */
    public Optional<Stored> download(String filename, String path) throws IOException {
        return internalDownload(new PathFile(path, filename));
    }


    /**
     * Obtiene la información de un archivo almacenado a partir de su ruta completa
     *
     * @param fullPath Ruta completa del archivo
     * @return Objeto que representa la información del archivo almacenado
     * @throws IOException Si ocurre un error de lectura o escritura al obtener la información del archivo
     */
    public Optional<Stored.Info> info(String fullPath) throws IOException {
        var split = SplitPath.from(fullPath);
        return info(split.filename(), split.path());
    }

    /**
     * Obtiene la información de un archivo almacenado a partir de su nombre y ruta
     *
     * @param filename Nombre del archivo
     * @param path     Ruta donde se encuentra el archivo
     * @return Objeto que representa la información del archivo almacenado
     * @throws IOException Si ocurre un error de lectura o escritura al obtener la información del archivo
     */
    public Optional<Stored.Info> info(String filename, String path) throws IOException {
        return internalInfo(new PathFile(path, filename));
    }

    /**
     * Verifica si un archivo almacenado existe a partir de su ruta completa
     *
     * @param fullPath Ruta completa del archivo
     * @return Si el archivo existe o no
     * @throws IOException Si ocurre un error de lectura o escritura al verificar la existencia del archivo
     */
    public boolean exists(String fullPath) throws IOException {
        var split = SplitPath.from(fullPath);
        return exists(split.filename(), split.path());
    }

    /**
     * Verifica si un archivo almacenado existe a partir de su nombre y ruta
     *
     * @param filename Nombre del archivo
     * @param path     Ruta donde se encuentra el archivo
     * @return Si el archivo existe o no
     * @throws IOException Si ocurre un error de lectura o escritura al verificar la existencia del archivo
     */
    public boolean exists(String filename, String path) throws IOException {
        var normalizedPath = StorageUtils.normalizePath(path);
        return internalExists(new PathFile(normalizedPath, filename));
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
        internalRemove(new PathFile(path, filename));
    }


    /**
     * Genera una URL de acceso limitado para un archivo almacenado.
     * <p>
     * La URL permite acceder al archivo de forma controlada durante el periodo definido
     * en {@code duration}. Suelen usarse para exponer temporalmente ciertos recursos
     * de manera pública o semipública.
     * </p>
     *
     * @param pathFile Objeto que contiene el nombre y la ruta del archivo dentro del storage.
     * @param duration Duración por la cual la URL será válida.
     * @return Una URL de acceso limitado al archivo.
     */
    protected abstract URL internalGenerateSignedUrl(PathFile pathFile, Duration duration);

    /**
     * Genera una URL de acceso limitado para un archivo almacenado a partir de su ruta completa.
     *
     * @param fullPath Ruta completa del archivo en el storage.
     * @param duration Duración por la cual la URL será válida.
     * @return Una URL de acceso limitado al archivo.
     */
    public URL generateSignedUrl(String fullPath, Duration duration) {
        var split = SplitPath.from(fullPath);
        return internalGenerateSignedUrl(new PathFile(split.path(), split.filename()), duration);
    }


    /**
     * Elimina los archivos almacenados a partir de un objeto que contiene las referencias a los archivos
     * a eliminar
     *
     * @param purgable Objeto que contiene las referencias a los archivos a eliminar
     * @throws IOException Si ocurre un error de lectura o escritura al eliminar los archivos almacenados a partir del objeto purgable
     * @throws FileNotFoundStorageException Si no se encuentra el archivo a eliminar
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
     * @throws IOException Si ocurre un error de lectura o escritura al eliminar los archivos almacenados a partir de los objetos purgables
     * @throws FileNotFoundStorageException Si no se encuentra alguno de los archivos a eliminar
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
        var pathFile = new PathFile(path, filename);
        var downloadedFile = internalDownload(pathFile);
        if (downloadedFile.isEmpty()) {
            throw new FileNotFoundStorageException(pathFile);
        }
        target.store(downloadedFile.get().getStream(), filename, path);
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

        public static SplitPath from(String fullPath) {
            var split = new SplitPath(extractPath(fullPath), extractFilename(fullPath));
            log.debug("Split path: {}", split);
            return split;
        }

        /**
         * Extrae la ruta de un archivo a partir de su ruta completa
         *
         * @param fullpath Ruta completa del archivo
         * @return Ruta del archivo
         */
        private static String extractPath(String fullpath) {
            return fullpath.contains("/") ? fullpath.substring(0, fullpath.lastIndexOf("/") + 1) : "/";
        }

        /**
         * Extrae el nombre de un archivo a partir de su ruta completa
         *
         * @param fullpath Ruta completa del archivo
         * @return Nombre del archivo
         */
        private static String extractFilename(String fullpath) {
            return fullpath.contains("/") ? fullpath.substring(fullpath.lastIndexOf("/") + 1) : fullpath;
        }

        @Override
        public String toString() {
            return "[path= " + path + ", filename= " + filename + "]";
        }
    }
}

