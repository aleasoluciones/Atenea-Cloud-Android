package com.ateneacloud.drive.sync.logs.repository.coreData;

import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.sync.enums.SeafSyncLogStatus;
import com.ateneacloud.drive.sync.logs.SeafSyncLog;
import com.ateneacloud.drive.sync.logs.database.SeafSyncLogRoomDatabase;
import com.ateneacloud.drive.sync.logs.repository.SeafSyncLogRepository;

import java.util.List;

public class SeafSyncLogCoreDataRepository implements SeafSyncLogRepository {

    private SeafSyncLogRoomDatabase seafSyncLogRoomDatabase;


    public SeafSyncLogCoreDataRepository() {
        seafSyncLogRoomDatabase = SeafSyncLogRoomDatabase.getInstance(SeadroidApplication.getAppContext());
    }

    @Override
    public List<SeafSyncLog> all() {
        return seafSyncLogRoomDatabase.seafSyncLogDao().all();
    }

    @Override
    public void insert(SeafSyncLog log) {
        try {
            seafSyncLogRoomDatabase.seafSyncLogDao().insert(log);
        } catch (Exception e) {
        }
    }

    @Override
    public void update(SeafSyncLog log) {
        try {
            seafSyncLogRoomDatabase.seafSyncLogDao().update(log);
        } catch (Exception e) {
        }
    }

    @Override
    public void remove(SeafSyncLog log) {
        try {
            seafSyncLogRoomDatabase.seafSyncLogDao().remove(log);
        } catch (Exception e) {
        }
    }

    @Override
    public SeafSyncLog find(String accountId, String resourceId, String repoId, String settingsId) {
        return seafSyncLogRoomDatabase.seafSyncLogDao().find(accountId, resourceId, repoId, settingsId);
    }

    @Override
    public List<SeafSyncLog> findBySettingsAndAccountId(String settingsId, String accountId) {
        return seafSyncLogRoomDatabase.seafSyncLogDao().findBySettingsAndAccountId(settingsId, accountId);
    }

    @Override
    public List<SeafSyncLog> findBySettingsAndAccountIdAndStatus(String settingsId, String accountId, Enum<SeafSyncLogStatus> status) {
        return seafSyncLogRoomDatabase.seafSyncLogDao().findBySettingsAndAccountIdAndStatus(settingsId, accountId, status);
    }

    @Override
    public void clear() {
        seafSyncLogRoomDatabase.seafSyncLogDao().clear();
    }
}
