package com.ateneacloud.drive.sync.settings;

import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.sync.enums.SeafSyncStatus;
import com.ateneacloud.drive.sync.settings.repository.SeafSyncSettingsRepository;
import com.ateneacloud.drive.sync.settings.repository.impl.DefaultSeafSyncSettingsRepository;
import com.ateneacloud.drive.util.NetworkUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SeafSyncSettingsService {
    private SeafSyncSettingsRepository settingsRepository;

    public SeafSyncSettingsService() {
        this.settingsRepository = new DefaultSeafSyncSettingsRepository();
    }

    public List<SeafSyncSettings> getSettings() {
        return all();
    }

    public boolean exists(String settingsId) {
        boolean exists = settingsRepository.findById(settingsId) != null ? true : false;
        return exists;
    }

    public void add(SeafSyncSettings setting) {
        settingsRepository.insert(setting);
    }

    public void remove(SeafSyncSettings setting) {
        settingsRepository.remove(setting);
    }

    public void update(SeafSyncSettings setting) {
        settingsRepository.update(setting);
    }


    public void clear() {
        settingsRepository.clear();
    }

    public List<SeafSyncSettings> all() {
        List<SeafSyncSettings> allSettings = settingsRepository.all();
        return allSettings;
    }

    public List<SeafSyncSettings> allToAccount(Account account) {
        List<SeafSyncSettings> allSettings = settingsRepository.all();
        List<SeafSyncSettings> filterSettings = new ArrayList<>();
        for (SeafSyncSettings setting : allSettings) {
            if (setting.getAccountId().equals(account.getEmail())) {
                filterSettings.add(setting);
            }
        }
        return filterSettings;
    }

    public List<SeafSyncSettings> activeSettings() {
        List<SeafSyncSettings> allSettings = all();
        List<SeafSyncSettings> activeSettings = new ArrayList<>();
        for (SeafSyncSettings setting : allSettings) {
            if (setting.isActive()) {
                if (setting.isUploadOnlyOverWifi()) {
                    if (NetworkUtils.isConnectedToWifi()) {
                        activeSettings.add(setting);
                    }
                } else {
                    activeSettings.add(setting);
                }
            }
        }
        return activeSettings;
    }

    public void updateState(SeafSyncSettings setting, Enum<SeafSyncStatus> state) {
        setting.setStatus(state);
        settingsRepository.update(setting);
    }

    public void setLastRunTime(SeafSyncSettings setting, boolean isBasic) {
        Date currentDate = new Date();

        if (!isBasic) {
            if (setting.getExpireDate() != null) {
                if (currentDate.after(setting.getExpireDate()) || currentDate.equals(setting.getExpireDate())) {
                    setting.setActive(false);
                }
            }
        }


        setting.setLastExecution(currentDate);
        settingsRepository.update(setting);
    }

    public boolean isSameOriginDestination(SeafSyncSettings settings) {
        List<SeafSyncSettings> allSettings = new ArrayList<>();
        allSettings = settingsRepository.findByOriginAndDestination(settings);

        if(!allSettings.isEmpty() && allSettings.size() == 1 && allSettings.get(0).getId().equals(settings.getId())){
            return false;
        }

        return !allSettings.isEmpty();
    }
}
