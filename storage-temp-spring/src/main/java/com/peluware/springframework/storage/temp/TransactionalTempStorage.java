package com.peluware.springframework.storage.temp;

import com.peluware.storage.Storage;
import com.peluware.storage.StorageUtils;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;
import com.peluware.storage.temp.*;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

import static org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK;

/**
 * Extensión de {@link TempStorage} que coordina el movimiento de archivos con el
 * ciclo de vida de las transacciones de Spring.
 * <p>
 * En lugar de mover el archivo directamente (operación irreversible), realiza una
 * copia al destino y delega la limpieza al commit/rollback de la transacción:
 * <ul>
 *   <li><b>Commit</b>: elimina el archivo temporal y el backup (si existía).</li>
 *   <li><b>Rollback</b>: restaura el archivo original desde el backup (si existía)
 *       o elimina el archivo copiado al destino (si no había nada antes).</li>
 * </ul>
 * Debe usarse dentro de un contexto transaccional activo de Spring.
 */
public class TransactionalTempStorage extends TempStorage {

    public TransactionalTempStorage(Storage storage, TempUploadTicketManager ticketManager) {
        super(storage, ticketManager);
    }

    @Override
    protected void internalConfirm(String tempPath, String targetPath) throws IOException {
        var hadExisting = storage.exists(targetPath);
        final String backupPath;

        // si ya existe un archivo en el destino, lo respaldamos antes de sobrescribir
        // para poder restaurarlo en caso de rollback
        if (hadExisting) {
            backupPath = targetPath + ".bak";
            storage.copy(targetPath, backupPath);
        } else {
            backupPath = null;
        }

        try {
            storage.copy(tempPath, targetPath);
        } catch (StorageObjectNotFoundException e) {
            // el archivo temporal no existe, limpiamos el backup si lo creamos
            StorageUtils.removeQuietly(storage, backupPath);
            throw new TempUploadFileNotFoundException(tempPath, e);
        }

        // en commit: eliminamos el temporal y el backup (ya no se necesitan)
        StorageTransactionalUtils.removeAfterCommit(storage, tempPath, backupPath);

        if (hadExisting) {
            // en rollback: restauramos el archivo original desde el backup y eliminamos el backup
            StorageTransactionalUtils.onCompletion(STATUS_ROLLED_BACK, () -> {
                try {
                    storage.copy(backupPath, targetPath);
                } catch (IOException ignored) {
                }
                StorageUtils.removeQuietly(storage, backupPath);
            });
        } else {
            // en rollback: eliminamos el archivo copiado al destino, no había nada antes
            StorageTransactionalUtils.removeAfterRollback(storage, targetPath);
        }
    }

    /**
     * Confirma la subida y elimina el archivo existente si la ruta destino cambia.
     * Si el ticket es {@code null} o está en blanco, se interpreta como ausencia de
     * nueva subida y el archivo existente se programa para eliminar tras el commit.
     *
     * @param ticket       identificador del ticket, o {@code null}/{@code ""} para eliminar el existente
     * @param existingPath ruta del archivo actualmente almacenado, puede ser {@code null}
     * @return la nueva ruta del archivo confirmado, o {@code null} si se eliminó
     */
    public @Nullable String confirmAndReplace(@Nullable String ticket, @Nullable String existingPath) throws IOException {
        return confirmAndReplace(ticket, existingPath, TempStorageValidation.valid());
    }

    /**
     * Confirma la subida aplicando múltiples validaciones y elimina el archivo existente si la ruta cambia.
     *
     * @param ticket       identificador del ticket
     * @param existingPath ruta del archivo actualmente almacenado
     * @param validations  validaciones a aplicar antes de confirmar
     * @return la nueva ruta del archivo confirmado, o {@code null} si se eliminó
     */
    public @Nullable String confirmAndReplace(@Nullable String ticket, @Nullable String existingPath, TempStorageValidation... validations) throws IOException {
        return confirmAndReplace(ticket, existingPath, TempStorageValidation.composite(validations));
    }

    /**
     * Confirma la subida aplicando una validación y elimina el archivo existente si la ruta cambia.
     * <p>
     * Comportamiento según el valor del ticket:
     * <ul>
     *   <li>{@code null} o en blanco: programa la eliminación de {@code existingPath} tras commit y devuelve {@code null}.</li>
     *   <li>ticket válido: confirma la subida; si la nueva ruta difiere de {@code existingPath},
     *       programa la eliminación del existente tras commit.</li>
     * </ul>
     *
     * @param ticket       identificador del ticket
     * @param existingPath ruta del archivo actualmente almacenado
     * @param validation   validación a aplicar antes de confirmar
     * @return la nueva ruta del archivo confirmado, o {@code null} si se eliminó
     */
    public @Nullable String confirmAndReplace(@Nullable String ticket, @Nullable String existingPath, TempStorageValidation validation) throws IOException {
        if (ticket == null || ticket.isBlank()) {
            StorageTransactionalUtils.removeAfterCommit(storage, existingPath);
            return null;
        }
        var newKey = super.confirm(ticket, validation);
        if (!newKey.equals(existingPath)) {
            StorageTransactionalUtils.removeAfterCommit(storage, existingPath);
        }
        return newKey;
    }
}
