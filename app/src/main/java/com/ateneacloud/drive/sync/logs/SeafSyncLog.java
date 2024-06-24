package com.ateneacloud.drive.sync.logs;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.ateneacloud.drive.sync.enums.SeafSyncLogStatus;

import java.util.Date;

@Entity(tableName = "seafsynclogs", primaryKeys = {"resourceId", "repoId", "settingsId", "accountId"})
public class SeafSyncLog {

    @NonNull
    private String resourceId;
    @NonNull
    private String repoId;
    @NonNull
    private String settingsId;
    @NonNull
    private String accountId;
    private String resourceHash;
    private Date uploadedDate;
    private Enum<SeafSyncLogStatus> state;
    private String remoteIdentifier;
    private String remotePath;
    private String remoteName;

    public SeafSyncLog() {
        this.state = SeafSyncLogStatus.Unknow; // Set the initial upload state as needed
    }

    public SeafSyncLog(String accountId, String resourceId, String targetId, String settingsId, Date uploadedDate, String resourceHash) {
        this.accountId = accountId;
        this.resourceId = resourceId;
        this.repoId = targetId;
        this.settingsId = settingsId;
        this.uploadedDate = uploadedDate;
        this.resourceHash = resourceHash;
        this.state = SeafSyncLogStatus.Unknow; // Set the initial upload state as needed
    }


    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getSettingsId() {
        return settingsId;
    }

    public void setSettingsId(String settingsId) {
        this.settingsId = settingsId;
    }

    public String getResourceHash() {
        return resourceHash;
    }

    public void setResourceHash(String resourceHash) {
        this.resourceHash = resourceHash;
    }

    public Date getUploadedDate() {
        return uploadedDate;
    }

    public void setUploadedDate(Date uploadedDate) {
        this.uploadedDate = uploadedDate;
    }

    public Enum<SeafSyncLogStatus> getState() {
        return state;
    }

    public void setState(Enum<SeafSyncLogStatus> state) {
        this.state = state;
    }

    public String getRemoteIdentifier() {
        return remoteIdentifier;
    }

    public void setRemoteIdentifier(String remoteIdentifier) {
        this.remoteIdentifier = remoteIdentifier;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }
}

