package com.ateneacloud.drive.sync.settings.repository.impl;

import com.ateneacloud.drive.sync.enums.SeafSyncType;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.settings.repository.SeafSyncSettingsRepository;
import com.ateneacloud.drive.sync.settings.repository.coreData.SeafSyncSettingsCoreDataRepository;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DefaultSeafSyncSettingsRepository implements SeafSyncSettingsRepository {
    private List<SeafSyncSettings> settings;

    private SeafSyncSettingsCoreDataRepository settingsCoreDataRepository;

    public DefaultSeafSyncSettingsRepository() {
        settingsCoreDataRepository = new SeafSyncSettingsCoreDataRepository();
        all();
    }

    @Override
    public List<SeafSyncSettings> all() {
        if (settings != null) {
            return settings;
        }

        settings = loadSettingsFromStorage();
        return settings;
    }

    @Override
    public List<SeafSyncSettings> filter(Predicate<SeafSyncSettings> predicate) {
        return all().stream().filter(predicate).collect(Collectors.toList());
    }

    private List<SeafSyncSettings> loadSettingsFromStorage() {
        List<SeafSyncSettings> loadedSettings = settingsCoreDataRepository.all();
        return loadedSettings;
    }

    @Override
    public void insert(SeafSyncSettings setting) {
        settings.add(setting);
        settingsCoreDataRepository.insert(setting);
    }

    @Override
    public void update(SeafSyncSettings setting) {
        SeafSyncSettings settingToUpdate = findById(setting.getId());
        if (settingToUpdate != null) {
            settingsCoreDataRepository.update(setting);
        }
    }

    @Override
    public void remove(SeafSyncSettings setting) {
        SeafSyncSettings settingToRemove = findById(setting.getId());
        if (settingToRemove != null) {
            settings.remove(settingToRemove);
            settingsCoreDataRepository.remove(settingToRemove);
        }
    }

    @Override
    public void clear() {
        settings.clear();
        settingsCoreDataRepository.clear();
    }

    @Override
    public SeafSyncSettings findById(String settingId) {
        return all().stream()
                .filter(setting -> setting.getId().equals(settingId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<SeafSyncSettings> findByOriginAndDestination(SeafSyncSettings settings) {
        if (settings.getType().equals(SeafSyncType.Gallery)) {
            return all().stream()
                    .filter(setting ->
                            setting.getType().equals(SeafSyncType.Gallery) &&
                                    setting.getRepoPath().equals(settings.getRepoPath()))
                    .collect(Collectors.toList());
        } else {
            return all().stream()
                    .filter(setting ->
                            setting.getRepoPath().equals(settings.getRepoPath()) &&
                                    setting.getResourceUri().equals(settings.getResourceUri())
                    )
                    .collect(Collectors.toList());
        }
    }

    public void reload() {
        settings = null;
        all();
    }


}
