package com.ateneacloud.drive.sync.fileProvider.providers;

import com.ateneacloud.drive.sync.fileProvider.SeafSyncProviderProtocol;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.enums.SeafSyncMode;
import com.ateneacloud.drive.sync.fileProvider.syncItems.SeafSyncFileItem;
import com.ateneacloud.drive.util.ValidatesFiles;

import java.util.ArrayList;
import java.util.List;

public class SeafSyncFilterSettingsProvider implements SeafSyncProviderProtocol {

    private SeafSyncProviderProtocol provider;
    private SeafSyncSettings settings;

    public SeafSyncFilterSettingsProvider(SeafSyncSettings settings, SeafSyncProviderProtocol provider) {
        this.settings = settings;
        this.provider = provider;
    }

    @Override
    public List<SeafSyncFileItem> getFiles() {
        List<SeafSyncFileItem> files = provider.getFiles();
        List<SeafSyncFileItem> filteredFiles = new ArrayList<>();

        for (SeafSyncFileItem syncItem : files) {
            // If no video upload is allowed and this item is video => out!
            if (!settings.isUploadVideos() && ValidatesFiles.isVideoByFile(syncItem.getPath())) {
                continue;
            }

            // If mode is incremental, only return items newer than the creation setting date
            if (settings.getMode() == SeafSyncMode.Incremental &&
                    (syncItem.getCreationDate().before(settings.getCreationDate()) ||
                            syncItem.getModificationDate().before(settings.getCreationDate()))) {
                continue;
            }

            filteredFiles.add(syncItem);
        }

        return filteredFiles;
    }
}

