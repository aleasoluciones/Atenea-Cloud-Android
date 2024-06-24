package com.ateneacloud.drive.sync.observers;

import com.ateneacloud.drive.sync.observers.callbacks.SeafSyncObserverProtocolCallback;

public interface SeafSyncObserverProtocol {

    /**
     * Obtiene un identificador para el observador.
     *
     * @return Un string identificador.
     */
    String getObserverIdentifier();

    /**
     * Inicia el observador y registra un bloque de devolución de llamada que se ejecutará cuando se detecten cambios.
     *
     * @param callback Un bloque de devolución de llamada que se ejecutará cuando se detecten cambios.
     */
    void start(SeafSyncObserverProtocolCallback callback);

    /**
     * Detiene el observador.
     */
    void stop();
}
