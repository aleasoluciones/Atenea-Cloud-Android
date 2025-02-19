package com.ateneacloud.drive.transfer;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.common.collect.Lists;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.notification.UploadNotificationProvider;

import java.util.List;

/**
 * Upload task manager
 * <p/>
 */
public class UploadTaskManager extends TransferManager implements UploadStateListener {
    private static final String DEBUG_TAG = "UploadTaskManager";

    public static final String BROADCAST_FILE_UPLOAD_SUCCESS = "uploaded";
    public static final String BROADCAST_FILE_UPLOAD_FAILED = "uploadFailed";
    public static final String BROADCAST_FILE_UPLOAD_PROGRESS = "uploadProgress";
    public static final String BROADCAST_FILE_UPLOAD_CANCELLED = "uploadCancelled";

    private static UploadNotificationProvider mNotifyProvider;


    public int addTaskToQue(String source, UploadFile uploadFile, boolean byBlock) {
        if (uploadFile.getRepoID() == null || uploadFile.getRepoName() == null)
            return 0;

        // create a new one to avoid IllegalStateException
        UploadTask task = new UploadTask(source, ++notificationID, uploadFile, byBlock, this);
        addTaskToQue(task);
        return task.getTaskID();
    }

    public List<UploadTaskInfo> getNoneCameraUploadTaskInfos() {
        List<UploadTaskInfo> noneCameraUploadTaskInfos = Lists.newArrayList();
        List<UploadTaskInfo> uploadTaskInfos = (List<UploadTaskInfo>) getAllTaskInfoList();
        for (UploadTaskInfo uploadTaskInfo : uploadTaskInfos) {
            // use isCopyToLocal as a flag to mark a camera photo upload task if false
            // mark a file upload task if true
            if (!uploadTaskInfo.isCopyToLocal) {
                continue;
            }
            noneCameraUploadTaskInfos.add(uploadTaskInfo);
        }

        return noneCameraUploadTaskInfos;
    }

    public void retry(int taskID) {
        UploadTask task = (UploadTask) getTask(taskID);
        if (task == null || !task.canRetry())
            return;
        addTaskToQue(task.getSource(), task.getUploadFile(), false);
    }

    private void notifyProgress(int taskID) {
        UploadTaskInfo info = (UploadTaskInfo) getTaskInfo(taskID);
        if (info == null)
            return;

        // use isCopyToLocal as a flag to mark a camera photo upload task if false
        // mark a file upload task if true
        if (!info.isCopyToLocal)
            return;

        //Log.d(DEBUG_TAG, "notify key " + info.repoID);
        if (mNotifyProvider != null) {
            mNotifyProvider.updateNotification();
        }

    }

    public void saveUploadNotifProvider(UploadNotificationProvider provider) {
        mNotifyProvider = provider;
    }

    public boolean hasNotifProvider() {
        return mNotifyProvider != null;
    }

    public UploadNotificationProvider getNotifProvider() {
        if (hasNotifProvider())
            return mNotifyProvider;
        else
            return null;
    }

    public void cancelAllUploadNotification() {
        if (mNotifyProvider != null)
            mNotifyProvider.cancelNotification();
    }

    // -------------------------- listener method --------------------//
    @Override
    public void onFileUploadProgress(int taskID) {
        Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
                BROADCAST_FILE_UPLOAD_PROGRESS).putExtra("taskID", taskID);
        LocalBroadcastManager.getInstance(SeadroidApplication.getAppContext()).sendBroadcast(localIntent);
        notifyProgress(taskID);
    }

    @Override
    public void onFileUploaded(int taskID) {
        remove(taskID);
        doNext();
        Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
                BROADCAST_FILE_UPLOAD_SUCCESS).putExtra("taskID", taskID);
        LocalBroadcastManager.getInstance(SeadroidApplication.getAppContext()).sendBroadcast(localIntent);
        notifyProgress(taskID);
    }

    @Override
    public void onFileUploadCancelled(int taskID) {
        remove(taskID);
        doNext();
        Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
                BROADCAST_FILE_UPLOAD_CANCELLED).putExtra("taskID", taskID);
        LocalBroadcastManager.getInstance(SeadroidApplication.getAppContext()).sendBroadcast(localIntent);
        notifyProgress(taskID);
    }

    @Override
    public void onFileUploadFailed(int taskID) {
        remove(taskID);
        doNext();
        Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
                BROADCAST_FILE_UPLOAD_FAILED).putExtra("taskID", taskID);
        LocalBroadcastManager.getInstance(SeadroidApplication.getAppContext()).sendBroadcast(localIntent);
        notifyProgress(taskID);
    }

}
