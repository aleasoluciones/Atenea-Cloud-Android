package com.ateneacloud.drive.transfer;

/**
 * Download state listener
 *
 */
public interface DownloadStateListener {
    void onFileDownloadProgress(int taskID);
    void onFileDownloaded(int taskID);
    void onFileDownloadFailed(int taskID);
}
