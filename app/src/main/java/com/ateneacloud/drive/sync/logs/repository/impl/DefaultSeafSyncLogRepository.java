package com.ateneacloud.drive.sync.logs.repository.impl;

import com.ateneacloud.drive.sync.enums.SeafSyncLogStatus;
import com.ateneacloud.drive.sync.logs.SeafSyncLog;
import com.ateneacloud.drive.sync.logs.repository.SeafSyncLogRepository;
import com.ateneacloud.drive.sync.logs.repository.coreData.SeafSyncLogCoreDataRepository;

import java.util.List;

public class DefaultSeafSyncLogRepository implements SeafSyncLogRepository {

    private List<SeafSyncLog> seafSyncLogs;
    private SeafSyncLogCoreDataRepository syncLogCoreDataRepository;

    public DefaultSeafSyncLogRepository() {
        syncLogCoreDataRepository = new SeafSyncLogCoreDataRepository();
        all();
    }

    @Override
    public List<SeafSyncLog> all() {
        if (seafSyncLogs != null) {
            seafSyncLogs = syncLogCoreDataRepository.all();
        }

        return seafSyncLogs;
    }

    @Override
    public void insert(SeafSyncLog log) {
        syncLogCoreDataRepository.insert(log);
    }

    @Override
    public void update(SeafSyncLog log) {
        syncLogCoreDataRepository.update(log);
    }

    @Override
    public void remove(SeafSyncLog log) {
        syncLogCoreDataRepository.remove(log);
    }

    @Override
    public SeafSyncLog find(String accountId, String resourceId, String repoId, String settingsId) {
        return syncLogCoreDataRepository.find(accountId, resourceId, repoId, settingsId);
    }

    @Override
    public List<SeafSyncLog> findBySettingsAndAccountId(String settingsId, String accountId) {
        return syncLogCoreDataRepository.findBySettingsAndAccountId(settingsId, accountId);
    }

    @Override
    public List<SeafSyncLog> findBySettingsAndAccountIdAndStatus(String settingsId, String accountId, Enum<SeafSyncLogStatus> status) {
        return syncLogCoreDataRepository.findBySettingsAndAccountIdAndStatus(settingsId, accountId, status);
    }

    @Override
    public void clear() {
        syncLogCoreDataRepository.clear();
    }
}
