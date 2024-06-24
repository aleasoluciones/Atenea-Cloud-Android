package com.ateneacloud.drive.sync.fileProvider;

import android.net.Uri;

import com.ateneacloud.drive.sync.fileProvider.providers.SeafSyncFilterSettingsProvider;
import com.ateneacloud.drive.sync.fileProvider.providers.SeafSyncFolderProvider;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.enums.SeafSyncType;
import com.ateneacloud.drive.sync.fileProvider.providers.SeafSyncGalleryProvider;

import java.io.File;

public class SeafSyncFileProviderFactory {

    public static SeafSyncProviderProtocol getProviderFor(SeafSyncSettings settings) {

        if (settings.getType().equals(SeafSyncType.Folder)) {

            //return new SeafSyncFilterSettingsProvider(settings, createFolderProviderFor(settings));
            return createFolderProviderFor(settings);

        } else if (settings.getType().equals(SeafSyncType.Album)) {

            //return new SeafSyncFilterSettingsProvider(settings, new SeafSyncGalleryProvider(settings.getResourceUri()));
            return new SeafSyncGalleryProvider(settings.getResourceUri());

        } else if (settings.getType().equals(SeafSyncType.Gallery)) {

            //return new SeafSyncFilterSettingsProvider(settings, new SeafSyncGalleryProvider());
            return new SeafSyncGalleryProvider();

        }

        return null;

    }

    public static SeafSyncProviderProtocol getProviderForURI(Uri folderURI, SeafSyncSettings settings) {
        return new SeafSyncFilterSettingsProvider(settings, new SeafSyncFolderProvider(new File(folderURI.getPath().toString())));
    }

    private static SeafSyncFolderProvider createFolderProviderFor(SeafSyncSettings settings) {
        try {
            if (settings.getResourceUri() == null) {
                return null;
            }
            String path = settings.getResourceUri().getPath().toString();
            return new SeafSyncFolderProvider(new File(path));
        } catch (Exception e) {
            return null;
        }
    }
}
