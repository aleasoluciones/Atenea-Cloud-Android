package com.ateneacloud.drive.monitor;

import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.data.SeafCachedFile;

import java.io.File;

interface CachedFileChangedListener {
    void onCachedBlocksChanged(Account account, SeafCachedFile cf, File file);

    void onCachedFileChanged(Account account, SeafCachedFile cf, File file);
}

