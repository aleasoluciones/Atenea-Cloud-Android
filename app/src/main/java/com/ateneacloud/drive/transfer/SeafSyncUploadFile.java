package com.ateneacloud.drive.transfer;

import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.sync.settings.SeafSyncSettingsService;
import com.ateneacloud.drive.util.NetworkUtils;

public class SeafSyncUploadFile extends UploadFile {

    private String settingId;
    private boolean onlyWifi;
    private SeafSyncSettingsService settingsService;


    public SeafSyncUploadFile() {
        super();
        settingsService = new SeafSyncSettingsService();
        onlyWifi = false;

    }

    public SeafSyncUploadFile(Account account, String repoID, String repoName, String repoPath, String filePath, boolean update, boolean copyToLocal, String settingId, boolean onlyWifi) {
        super(account, repoID, repoName, repoPath, filePath, update, copyToLocal);
        this.settingId = settingId;
        this.onlyWifi = onlyWifi;
        settingsService = new SeafSyncSettingsService();
    }

    public String getSettingId() {
        return settingId;
    }

    public void setSettingId(String settingId) {
        this.settingId = settingId;
    }

    @Override
    public boolean isUpload() {
        return !NetworkUtils.isNotConnected() && (!onlyWifi || NetworkUtils.isConnectedToWifi());
    }

    @Override
    public boolean isMustBeCancelled() {
        return !settingsService.exists(settingId);
    }

}
