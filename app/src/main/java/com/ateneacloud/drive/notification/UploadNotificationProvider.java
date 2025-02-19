package com.ateneacloud.drive.notification;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.transfer.TaskState;
import com.ateneacloud.drive.transfer.TransferService;
import com.ateneacloud.drive.transfer.UploadTaskInfo;
import com.ateneacloud.drive.transfer.UploadTaskManager;
import com.ateneacloud.drive.ui.CustomNotificationBuilder;
import com.ateneacloud.drive.ui.activity.TransferActivity;

import java.util.List;

/**
 * Upload notification provider
 *
 */
public class UploadNotificationProvider extends BaseNotificationProvider {

    public UploadNotificationProvider(UploadTaskManager uploadTaskManager,
                                      TransferService transferService) {
        super(uploadTaskManager, transferService);

    }

    @Override
    protected String getProgressInfo() {
        String progressStatus = "";

        if (txService == null)
            return progressStatus;

        // failed or cancelled tasks won`t be shown in notification state
        // but failed or cancelled detailed info can be viewed in TransferList
        if (getState().equals(NotificationState.NOTIFICATION_STATE_COMPLETED_WITH_ERRORS))
            progressStatus = SeadroidApplication.getAppContext().getString(R.string.notification_upload_completed);
        else if (getState().equals(NotificationState.NOTIFICATION_STATE_COMPLETED))
            progressStatus = SeadroidApplication.getAppContext().getString(R.string.notification_upload_completed);
        else if (getState().equals(NotificationState.NOTIFICATION_STATE_PROGRESS)) {
            int uploadingCount = 0;
            List<UploadTaskInfo> infos = txService.getNoneCameraUploadTaskInfos();
            for (UploadTaskInfo info : infos) {
                if (info.state.equals(TaskState.INIT)
                        || info.state.equals(TaskState.TRANSFERRING))
                    uploadingCount++;
            }

            if (uploadingCount != 0)
                progressStatus = SeadroidApplication.getAppContext().getResources().
                                getQuantityString(R.plurals.notification_upload_info,
                                        uploadingCount,
                                        uploadingCount,
                                        getProgress());
        }
        return progressStatus;
    }

    @Override
    protected void notifyStarted() {
        Intent dIntent = new Intent(SeadroidApplication.getAppContext(), TransferActivity.class);
        dIntent.putExtra(NOTIFICATION_MESSAGE_KEY, NOTIFICATION_OPEN_UPLOAD_TAB);
        dIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent uPendingIntent = PendingIntent.getActivity(SeadroidApplication.getAppContext(),
                (int) System.currentTimeMillis(), dIntent, FLAG_IMMUTABLE);
        mNotifBuilder = CustomNotificationBuilder.getNotificationBuilder(SeadroidApplication.getAppContext(),
                CustomNotificationBuilder.CHANNEL_ID_UPLOAD)
                .setSmallIcon(R.drawable.ic_launcher)
                .setOnlyAlertOnce(true)
                .setContentTitle(SeadroidApplication.getAppContext().getString(R.string.notification_upload_started_title))
                .setOngoing(true)
                .setContentText(SeadroidApplication.getAppContext().getString(R.string.notification_upload_started_title))
                .setContentIntent(uPendingIntent)
                .setProgress(100, 0, false);

        // Make this service run in the foreground, supplying the ongoing
        // notification to be shown to the user while in this state.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            txService.startForeground(NOTIFICATION_ID_UPLOAD, mNotifBuilder.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            txService.startForeground(NOTIFICATION_ID_UPLOAD, mNotifBuilder.build());
        }

    }

    @Override
    protected int getProgress() {
        long uploadedSize = 0l;
        long totalSize = 0l;
        if (txService == null)
            return 0;

        List<UploadTaskInfo> infos = txService.getNoneCameraUploadTaskInfos();
        for (UploadTaskInfo info : infos) {
            if (info == null)
                continue;
            uploadedSize += info.uploadedSize;
            totalSize += info.totalSize;
        }

        if (totalSize == 0)
            return 0;

        return (int) (uploadedSize * 100 / totalSize);
    }

    @Override
    protected NotificationState getState() {
        if (txService == null)
            return NotificationState.NOTIFICATION_STATE_COMPLETED;

        List<UploadTaskInfo> infos = txService.getNoneCameraUploadTaskInfos();

        int progressCount = 0;
        int errorCount = 0;

        for (UploadTaskInfo info : infos) {
            if (info == null)
                continue;
            if (info.state.equals(TaskState.INIT)
                    || info.state.equals(TaskState.TRANSFERRING))
                progressCount++;
            else if (info.state.equals(TaskState.FAILED)
                    || info.state.equals(TaskState.CANCELLED))
                errorCount++;
        }

        if (progressCount == 0 && errorCount == 0)
            return NotificationState.NOTIFICATION_STATE_COMPLETED;
        else if (progressCount == 0 && errorCount > 0)
            return NotificationState.NOTIFICATION_STATE_COMPLETED_WITH_ERRORS;
        else // progressCount > 0
            return NotificationState.NOTIFICATION_STATE_PROGRESS;
    }

    @Override
    protected int getNotificationID() {
        return NOTIFICATION_ID_UPLOAD;
    }

    @Override
    protected String getNotificationTitle() {
        return SeadroidApplication.getAppContext().getString(R.string.notification_upload_started_title);
    }

}
