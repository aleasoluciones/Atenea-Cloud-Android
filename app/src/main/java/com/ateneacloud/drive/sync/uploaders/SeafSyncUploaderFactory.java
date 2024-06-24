package com.ateneacloud.drive.sync.uploaders;


import com.ateneacloud.drive.SeafConnection;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.enums.SeafSyncType;
import com.ateneacloud.drive.sync.uploaders.impl.SeafSyncGalleryUploader;
import com.ateneacloud.drive.sync.uploaders.impl.SeafSyncTreeUploader;

/**
 * Factory class for creating and returning uploaders based on the specified connection and synchronization settings.
 */
public class SeafSyncUploaderFactory {

    /**
     * Creates and returns an instance of an uploader based on the specified connection and synchronization settings.
     *
     * @param connection The SeafConnection instance to be used for the upload.
     * @param settings   The SeafSyncSettings object representing the synchronization settings for the uploader.
     * @return An instance conforming to the SeafUploaderProtocol for handling the upload.
     */
    public static SeafUploaderProtocol getUploaderFor(SeafConnection connection, SeafSyncSettings settings) {
        if (settings.getType().equals(SeafSyncType.Folder)) {

            return new SeafSyncTreeUploader(settings, connection);

        } else if (settings.getType().equals(SeafSyncType.Album) || settings.getType().equals(SeafSyncType.Gallery)) {

            return new SeafSyncGalleryUploader(settings, connection);

        }
        return null;
    }
}
