package com.ateneacloud.drive.sync.settings.repository;

import com.ateneacloud.drive.sync.settings.SeafSyncSettings;

import java.util.List;
import java.util.function.Predicate;

public interface SeafSyncSettingsRepository {
    List<SeafSyncSettings> all();

    List<SeafSyncSettings> filter(Predicate<SeafSyncSettings> predicate);

    void insert(SeafSyncSettings setting);

    void update(SeafSyncSettings setting);

    void remove(SeafSyncSettings setting);

    void clear();

    SeafSyncSettings findById(String settingsId);

    List<SeafSyncSettings> findByOriginAndDestination(SeafSyncSettings settings);
}
