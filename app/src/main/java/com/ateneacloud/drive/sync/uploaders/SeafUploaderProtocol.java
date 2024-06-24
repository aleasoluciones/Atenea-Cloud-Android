package com.ateneacloud.drive.sync.uploaders;

import com.ateneacloud.drive.SeafConnection;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;

public interface SeafUploaderProtocol {

    /**
     * Initializes a new instance of the uploader with specified synchronization settings and connection.
     *
     * @param settings    The SeafSyncSettings object representing the synchronization settings for the uploader.
     * @param connection  The SeafConnection instance to be used for the upload.
     * @return An instance conforming to the SeafUploaderProtocol.
     */
    public Object init(SeafSyncSettings settings, SeafConnection connection);

    /**
     * Starts the upload process.
     *
     * This method initiates the file upload according to the implemented logic in the uploader.
     */
    public void upload();
}
