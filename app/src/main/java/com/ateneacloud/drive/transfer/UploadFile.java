package com.ateneacloud.drive.transfer;

import com.ateneacloud.drive.account.Account;

public abstract class UploadFile {

    private Account account;
    private String repoID;
    private String repoName;

    private String repoPath;

    private String filePath;

    private boolean update;

    private boolean copyToLocal;

    public UploadFile() {
        update = false;
        copyToLocal = false;
    }

    public UploadFile(Account account, String repoID, String repoName, String repoPath, String filePath, boolean update, boolean copyToLocal) {
        this.account = account;
        this.repoID = repoID;
        this.repoName = repoName;
        this.repoPath = repoPath;
        this.filePath = filePath;
        this.update = update;
        this.copyToLocal = copyToLocal;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getRepoID() {
        return repoID;
    }

    public void setRepoID(String repoID) {
        this.repoID = repoID;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public boolean isCopyToLocal() {
        return copyToLocal;
    }

    public void setCopyToLocal(boolean copyToLocal) {
        this.copyToLocal = copyToLocal;
    }

    public abstract boolean isUpload();

    public abstract boolean isMustBeCancelled();
}
