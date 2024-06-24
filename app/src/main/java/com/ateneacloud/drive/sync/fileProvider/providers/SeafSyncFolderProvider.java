package com.ateneacloud.drive.sync.fileProvider.providers;

import com.ateneacloud.drive.sync.fileProvider.SeafSyncProviderProtocol;
import com.ateneacloud.drive.sync.fileProvider.syncItems.SeafSyncFileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SeafSyncFolderProvider implements SeafSyncProviderProtocol {

    private File folderFile;

    public SeafSyncFolderProvider(File folderFile) {
        this.folderFile = folderFile;
    }

    @Override
    public List<SeafSyncFileItem> getFiles() {
        List<SeafSyncFileItem> files = new ArrayList<>();
        File[] contents = folderFile.listFiles();

        if (contents != null) {
            for (File content : contents) {
                files.add(new SeafSyncFileItem(content));
            }
        }

        return files;
    }
}
