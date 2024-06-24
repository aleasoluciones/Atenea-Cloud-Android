package com.ateneacloud.drive.data;

public interface ProgressMonitor {
    void onProgressNotify(long total, boolean updateTotal);

    boolean isCancelled();
}
