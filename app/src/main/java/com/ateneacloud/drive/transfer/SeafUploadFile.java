package com.ateneacloud.drive.transfer;

import com.ateneacloud.drive.account.Account;

public class SeafUploadFile extends UploadFile {

    public SeafUploadFile() {
        super();
    }

    public SeafUploadFile(Account account, String repoID, String repoName, String repoPath, String filePath, boolean update, boolean copyToLocal) {
        super(account, repoID, repoName, repoPath, filePath, update, copyToLocal);
    }

    @Override
    public boolean isUpload() {
        return true;
    }

    @Override
    public boolean isMustBeCancelled() {
        return false;
    }
}
