package com.ateneacloud.drive.sync.logs.services;

import com.ateneacloud.drive.sync.SeafSyncUtils;
import com.ateneacloud.drive.sync.enums.SeafSyncLogStatus;
import com.ateneacloud.drive.sync.fileProvider.syncItems.SeafSyncFileItem;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.logs.SeafSyncLog;
import com.ateneacloud.drive.sync.logs.repository.impl.DefaultSeafSyncLogRepository;
import com.ateneacloud.drive.sync.logs.repository.SeafSyncLogRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

public class SeafSyncLogsService {
    private SeafSyncLogRepository repository;

    public SeafSyncLogsService() {
        this.repository = new DefaultSeafSyncLogRepository();
    }

    public SeafSyncLogsService(SeafSyncLogRepository repository) {
        this.repository = repository;
    }

    public boolean isAlreadyUploaded(SeafSyncFileItem file, SeafSyncSettings settings) {
        SeafSyncLog log = map(file, settings);
        if (log != null) {
            String accountId = settings.getAccountId();
            String resourceId = log.getResourceId();
            String targetId = log.getRepoId();
            String settingsId = settings.getId();
            SeafSyncLog seafSyncLog = repository.find(accountId, resourceId, targetId, settingsId);
            if (seafSyncLog != null) {
                return seafSyncLog.getState() == SeafSyncLogStatus.Uploaded || seafSyncLog.getState() == SeafSyncLogStatus.Deleted;
            }
        }
        return false;
    }

    public boolean log(SeafSyncFileItem file, SeafSyncSettings settings) {
        SeafSyncLog log = map(file, settings);
        if (log != null && repository.find(log.getAccountId(), log.getResourceId(), log.getRepoId(), log.getSettingsId()) == null) {
            repository.insert(log);
            return true;
        }
        return false;
    }

    public boolean log(SeafSyncFileItem file, SeafSyncSettings settings, String remotePath) {
        SeafSyncLog log = map(file, settings, remotePath);
        if (log != null && repository.find(log.getAccountId(), log.getResourceId(), log.getRepoId(), log.getSettingsId()) == null) {
            repository.insert(log);
            return true;
        }
        return false;
    }

    public boolean changeState(SeafSyncFileItem file, SeafSyncSettings settings, Enum<SeafSyncLogStatus> newState) {
        SeafSyncLog log = map(file, settings);
        if (log != null) {
            String accountId = settings.getAccountId();
            String resourceId = log.getResourceId();
            String targetId = log.getRepoId();
            String settingsId = settings.getId();
            SeafSyncLog seafSyncLog = repository.find(accountId, resourceId, targetId, settingsId);
            if (seafSyncLog != null) {
                seafSyncLog.setState(newState);
                repository.update(seafSyncLog);
                return true;
            }
        }
        return false;
    }

    public boolean markAsUploaded(SeafSyncFileItem file, SeafSyncSettings settings, String id) {
        SeafSyncLog log = map(file, settings);
        if (log != null) {
            String accountId = settings.getAccountId();
            String resourceId = log.getResourceId();
            String targetId = log.getRepoId();
            String settingsId = settings.getId();
            SeafSyncLog seafSyncLog = repository.find(accountId, resourceId, targetId, settingsId);
            if (seafSyncLog != null) {
                seafSyncLog.setUploadedDate(new Date());
                seafSyncLog.setRemoteIdentifier(id);
                seafSyncLog.setState(SeafSyncLogStatus.Uploaded);
                repository.update(seafSyncLog);
                return true;
            }
        }
        return false;
    }

    public boolean changeState(SeafSyncLog log, Enum<SeafSyncLogStatus> newState) {
        try {
            log.setState(newState);
            repository.update(log);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Enum<SeafSyncLogStatus> getState(SeafSyncFileItem file, SeafSyncSettings settings) {
        SeafSyncLog tempLog = map(file, settings);

        if (tempLog != null) {
            SeafSyncLog log = repository.find(settings.getAccountId(), tempLog.getResourceId(), settings.getRepoPath(), settings.getId());
            if (log != null) {
                return log.getState();
            }

        }

        return null;
    }

    public void clear() {
        repository.clear();
    }

    public List<SeafSyncLog> all() {
        return repository.all();
    }

    private SeafSyncLog createSeafSyncLog(SeafSyncFileItem file, SeafSyncSettings settings, String remotePath) {
        SeafSyncLog log = new SeafSyncLog();
        log.setResourceId(file.getIdentifier() + "/" + composeResourceIdFromLocalPath(file.getPath().getAbsolutePath()));
        log.setRepoId(settings.getRepoId());
        log.setSettingsId(settings.getId());
        log.setUploadedDate(new Date());
        log.setAccountId(settings.getAccountId());
        log.setRemotePath(remotePath);
        log.setRemoteName(file.getPath().getName());
        log.setRemoteIdentifier("");
        try {
            log.setResourceHash(SeafSyncUtils.calculateHash(log.getResourceId() + "|" + Files.size(Paths.get(file.getPath().getAbsolutePath()))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return log;
    }

    private SeafSyncLog map(SeafSyncFileItem file, SeafSyncSettings settings) {
        return createSeafSyncLog(file, settings, settings.getRepoPath());
    }

    private SeafSyncLog map(SeafSyncFileItem file, SeafSyncSettings settings, String remotePath) {
        return createSeafSyncLog(file, settings, remotePath);
    }

    private String composeResourceIdFromLocalPath(String path) {
        return new File(path).getName();
    }

    public List<SeafSyncLog> findBySettingsAndAccountId(String settingsId, String accountId) {
        return repository.findBySettingsAndAccountId(settingsId, accountId);
    }

    public List<SeafSyncLog> findBySettingsAndAccountIdAndStatus(String settingsId, String accountId, Enum<SeafSyncLogStatus> status) {
        return repository.findBySettingsAndAccountIdAndStatus(settingsId, accountId, status);
    }
}
