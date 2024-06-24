package com.ateneacloud.drive.sync.observers;

import com.ateneacloud.drive.sync.observers.impl.SeafSyncBlindObserver;
import com.ateneacloud.drive.sync.observers.impl.SeafSyncGalleryObserver;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.enums.SeafSyncType;
import com.ateneacloud.drive.sync.observers.impl.SeafSyncFolderObserver;

/**
 * Factory class for creating synchronization observers based on the provided SeafSyncSettings.
 */
public class SeafSyncObserverFactory {

    /**
     * Crea un observador de sincronización según la configuración de SeafSyncSettings.
     *
     * @param setting La configuración de SeafSyncSettings para la cual crear un observador.
     * @return Un objeto que cumple con el protocolo SeafSyncObserverProtocol.
     */
    public static SeafSyncObserverProtocol createFor(SeafSyncSettings setting) {

        if (setting.getType().equals(SeafSyncType.Folder)) {

            return new SeafSyncFolderObserver(setting);

        } else if (setting.getType().equals(SeafSyncType.Album) || setting.getType().equals(SeafSyncType.Gallery)) {

            return new SeafSyncGalleryObserver(setting);

        }
        return new SeafSyncBlindObserver();
    }
}
