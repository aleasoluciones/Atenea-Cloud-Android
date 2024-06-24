package com.ateneacloud.drive.sync.logs.repository;

import com.ateneacloud.drive.sync.enums.SeafSyncLogStatus;
import com.ateneacloud.drive.sync.logs.SeafSyncLog;

import java.util.List;

public interface SeafSyncLogRepository {

    List<SeafSyncLog> all();

    void insert(SeafSyncLog log);

    void update(SeafSyncLog log);

    void remove(SeafSyncLog log);

    SeafSyncLog find(String accountId ,String  resourceId, String repoId, String settingsId);

    List<SeafSyncLog> findBySettingsAndAccountId(String settingsId, String accountId);

    List<SeafSyncLog> findBySettingsAndAccountIdAndStatus(String settingsId, String accountId, Enum<SeafSyncLogStatus> status);

    void clear();
}