package com.ateneacloud.drive.sync.fileProvider;

import com.ateneacloud.drive.sync.fileProvider.syncItems.SeafSyncFileItem;

import java.util.List;

public interface SeafSyncProviderProtocol {
    public List<SeafSyncFileItem> getFiles();
    //cambio
}
