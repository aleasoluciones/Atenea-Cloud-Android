package com.ateneacloud.drive.sync.tree.treeFolderState.repository.coreData;

import android.net.Uri;

import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.sync.tree.treeFolderState.SeafSyncTreeState;
import com.ateneacloud.drive.sync.tree.treeFolderState.repository.SeafSyncTreeFolderStateRepository;
import com.ateneacloud.drive.sync.tree.treeFolderState.database.SeafSyncTreeFolderStateRoomDatabase;

import java.util.List;

public class SeafSyncTreeFolderStateCoreDataRepository implements SeafSyncTreeFolderStateRepository {

    private SeafSyncTreeFolderStateRoomDatabase seafSyncTreeFolderStateRoomDatabase;


    public SeafSyncTreeFolderStateCoreDataRepository() {
        seafSyncTreeFolderStateRoomDatabase = SeafSyncTreeFolderStateRoomDatabase.getInstance(SeadroidApplication.getAppContext());
    }

    @Override
    public List<SeafSyncTreeState> all() {
        return seafSyncTreeFolderStateRoomDatabase.seafSyncTreeFolderStateDao().all();
    }

    @Override
    public void insert(SeafSyncTreeState state) {
        seafSyncTreeFolderStateRoomDatabase.seafSyncTreeFolderStateDao().insert(state);
    }

    @Override
    public void update(SeafSyncTreeState state) {
        seafSyncTreeFolderStateRoomDatabase.seafSyncTreeFolderStateDao().update(state);
    }

    @Override
    public void remove(SeafSyncTreeState state) {
        seafSyncTreeFolderStateRoomDatabase.seafSyncTreeFolderStateDao().remove(state);
    }

    @Override
    public List<SeafSyncTreeState> find(String identifier) {
        return seafSyncTreeFolderStateRoomDatabase.seafSyncTreeFolderStateDao().find(identifier);
    }

    @Override
    public List<SeafSyncTreeState> findBySyncSetting(String syncSettingId) {
        return seafSyncTreeFolderStateRoomDatabase.seafSyncTreeFolderStateDao().findBySyncSetting(syncSettingId);
    }

    @Override
    public List<SeafSyncTreeState> findByUri(Uri uri) {
        return seafSyncTreeFolderStateRoomDatabase.seafSyncTreeFolderStateDao().findByUri(uri);
    }

    @Override
    public void clear() {
        seafSyncTreeFolderStateRoomDatabase.seafSyncTreeFolderStateDao().clear();
    }
}
