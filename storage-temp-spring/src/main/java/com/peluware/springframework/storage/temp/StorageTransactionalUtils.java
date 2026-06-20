package com.peluware.springframework.storage.temp;

import com.peluware.storage.Storage;
import com.peluware.storage.StorageUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.stream.Stream;

import static org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK;

/**
 * Utilidades para coordinar operaciones de storage con el ciclo de vida de las
 * transacciones de Spring.
 * <p>
 * Dado que los backends de storage (S3, GCS, disco) no participan en transacciones
 * relacionales, estos métodos permiten diferir la limpieza de archivos al momento
 * del commit o del rollback de la transacción activa, logrando consistencia eventual.
 * <p>
 * Si no hay ninguna sincronización de transacción activa
 * ({@link TransactionSynchronizationManager#isSynchronizationActive()} devuelve {@code false}),
 * las operaciones programadas se ignoran silenciosamente.
 */
public final class StorageTransactionalUtils {

    private StorageTransactionalUtils() {
        throw new UnsupportedOperationException("Utility Class");
    }

    /**
     * Registra una acción para ejecutarse cuando la transacción activa complete con el estado indicado.
     *
     * @param status estado de completación que activa la acción
     *               ({@link TransactionSynchronization#STATUS_COMMITTED} o {@link TransactionSynchronization#STATUS_ROLLED_BACK})
     * @param action acción a ejecutar
     */
    public static void onCompletion(int status, Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int completionStatus) {
                if (completionStatus == status) {
                    action.run();
                }
            }
        });
    }

    /**
     * Programa la eliminación de las rutas indicadas cuando la transacción complete con el estado dado.
     * Las rutas {@code null} se ignoran silenciosamente.
     *
     * @param status  estado de completación que activa la eliminación
     * @param storage backend de storage donde se eliminarán los archivos
     * @param paths   rutas a eliminar
     */
    public static void removeAfterCompletion(int status, Storage storage, @Nullable String @Nullable ... paths) {
        if (paths == null) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        onCompletion(status, () -> StorageUtils.removeQuietly(storage, paths));
    }

    /**
     * Programa la eliminación de los archivos indicados tras el commit de la transacción.
     * Útil para limpiar archivos temporales u obsoletos una vez que la operación principal tuvo éxito.
     *
     * @param storage backend de storage
     * @param paths   rutas a eliminar tras el commit
     */
    public static void removeAfterCommit(Storage storage, @Nullable String @Nullable ... paths) {
        removeAfterCompletion(STATUS_COMMITTED, storage, paths);
    }

    /**
     * Programa la eliminación de los archivos indicados tras el rollback de la transacción.
     * Útil para deshacer archivos copiados al destino cuando la transacción falla.
     *
     * @param storage backend de storage
     * @param paths   rutas a eliminar tras el rollback
     */
    public static void removeAfterRollback(Storage storage, @Nullable String @Nullable ... paths) {
        removeAfterCompletion(STATUS_ROLLED_BACK, storage, paths);
    }

    /** @see #removeAfterCommit(Storage, String...) */
    public static void removeAfterCommit(Storage storage, Collection<@Nullable String> paths) {
        removeAfterCompletion(STATUS_COMMITTED, storage, paths.toArray(String[]::new));
    }

    /** @see #removeAfterRollback(Storage, String...) */
    public static void removeAfterRollback(Storage storage, Collection<@Nullable String> paths) {
        removeAfterCompletion(STATUS_ROLLED_BACK, storage, paths.toArray(String[]::new));
    }

    /** @see #removeAfterCommit(Storage, String...) */
    public static void removeAfterCommit(Storage storage, Stream<@Nullable String> paths) {
        removeAfterCompletion(STATUS_COMMITTED, storage, paths.toArray(String[]::new));
    }

    /** @see #removeAfterRollback(Storage, String...) */
    public static void removeAfterRollback(Storage storage, Stream<@Nullable String> paths) {
        removeAfterCompletion(STATUS_ROLLED_BACK, storage, paths.toArray(String[]::new));
    }
}
