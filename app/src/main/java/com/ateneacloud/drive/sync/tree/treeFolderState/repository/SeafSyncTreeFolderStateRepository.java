package com.ateneacloud.drive.sync.tree.treeFolderState.repository;

import android.net.Uri;

import com.ateneacloud.drive.sync.tree.treeFolderState.SeafSyncTreeState;

import java.util.List;

public interface SeafSyncTreeFolderStateRepository {
    List<SeafSyncTreeState> all();
    void insert(SeafSyncTreeState state);
    void update(SeafSyncTreeState state);
    void remove(SeafSyncTreeState state);
    List<SeafSyncTreeState> find(String identifier);
    List<SeafSyncTreeState> findBySyncSetting(String syncSettingId);
    List<SeafSyncTreeState> findByUri(Uri uri);
    void clear();
}
