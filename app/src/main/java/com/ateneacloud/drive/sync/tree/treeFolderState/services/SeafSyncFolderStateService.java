package com.ateneacloud.drive.sync.tree.treeFolderState.services;

import android.net.Uri;

import com.ateneacloud.drive.sync.tree.treeFolderState.SeafSyncTreeState;
import com.ateneacloud.drive.sync.tree.treeFolderState.repository.SeafSyncTreeFolderStateRepository;
import com.ateneacloud.drive.sync.tree.treeFolderState.repository.coreData.SeafSyncTreeFolderStateCoreDataRepository;

import java.util.List;

public class SeafSyncFolderStateService {

    private SeafSyncTreeFolderStateRepository repository;

    public SeafSyncFolderStateService() {
        this.repository = new SeafSyncTreeFolderStateCoreDataRepository();
    }

    public SeafSyncFolderStateService(SeafSyncTreeFolderStateRepository repository) {
        this.repository = repository;
    }

    public void insert(SeafSyncTreeState state) {
        repository.insert(state);
    }

    public void update(SeafSyncTreeState state) {
        repository.update(state);
    }

    public void remove(SeafSyncTreeState state) {
        repository.remove(state);
    }

    public List<SeafSyncTreeState> all() {
        return repository.all();
    }

    public void clear() {
        repository.clear();
    }

    public List<SeafSyncTreeState> findByUri(Uri uri) {
        return repository.findByUri(uri);
    }

    public List<SeafSyncTreeState> findBySyncSetting(String syncSettingId) {
        return repository.findBySyncSetting(syncSettingId);
    }

    public List<SeafSyncTreeState> find(String identifier) {
        return repository.find(identifier);
    }
}
