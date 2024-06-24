package com.ateneacloud.drive.sync.settings.repository.coreData;

import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.settings.database.SeafSyncSettingsConverter;
import com.ateneacloud.drive.sync.settings.database.SeafSyncSettingsRoomDatabase;
import com.ateneacloud.drive.sync.settings.repository.SeafSyncSettingsRepository;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SeafSyncSettingsCoreDataRepository implements SeafSyncSettingsRepository {

    private SeafSyncSettingsRoomDatabase seafSyncSettingsRoomDatabase;


    public SeafSyncSettingsCoreDataRepository() {
        seafSyncSettingsRoomDatabase = SeafSyncSettingsRoomDatabase.getInstance(SeadroidApplication.getAppContext());
    }

    @Override
    public List<SeafSyncSettings> all() {
        return seafSyncSettingsRoomDatabase.seafSyncSettingsDao().all();
    }

    @Override
    public List<SeafSyncSettings> filter(Predicate<SeafSyncSettings> predicate) {
        return all().stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public void insert(SeafSyncSettings setting) {
        seafSyncSettingsRoomDatabase.seafSyncSettingsDao().insert(setting);
    }

    @Override
    public void update(SeafSyncSettings setting) {
        seafSyncSettingsRoomDatabase.seafSyncSettingsDao().update(setting);
    }

    @Override
    public void remove(SeafSyncSettings setting) {
        seafSyncSettingsRoomDatabase.seafSyncSettingsDao().remove(setting);
    }

    @Override
    public void clear() {
        seafSyncSettingsRoomDatabase.seafSyncSettingsDao().clear();
    }

    @Override
    public SeafSyncSettings findById(String settingsId) {
        return seafSyncSettingsRoomDatabase.seafSyncSettingsDao().find(settingsId);
    }

    @Override
    public List<SeafSyncSettings> findByOriginAndDestination(SeafSyncSettings settings) {
        String resourceUri = SeafSyncSettingsConverter.uriToString(settings.getResourceUri());
        return seafSyncSettingsRoomDatabase.seafSyncSettingsDao().findByOriginAndDestination(settings.getRepoId(), settings.getRepoPath(), resourceUri);
    }
}
