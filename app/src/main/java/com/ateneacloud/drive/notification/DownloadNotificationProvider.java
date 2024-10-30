package com.ateneacloud.drive.notification;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
import static androidx.media3.exoplayer.offline.DownloadService.startForeground;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.transfer.DownloadTaskInfo;
import com.ateneacloud.drive.transfer.DownloadTaskManager;
import com.ateneacloud.drive.transfer.TaskState;
import com.ateneacloud.drive.transfer.TransferService;
import com.ateneacloud.drive.ui.CustomNotificationBuilder;
import com.ateneacloud.drive.ui.activity.TransferActivity;

import java.util.List;

/**
 * Download notification provider
 *
 */
public class DownloadNotificationProvider extends BaseNotificationProvider {

    public DownloadNotificationProvider(DownloadTaskManager downloadTaskManager,
                                        TransferService transferService) {
        super(downloadTaskManager, transferService);
    }

    @Override
    protected String getProgressInfo() {
        String progressStatus = "";

        if (txService == null)
            return progressStatus;

        // failed or cancelled tasks won`t be shown in notification state
        // but failed or cancelled detailed info can be viewed in TransferList
        if (getState().equals(NotificationState.NOTIFICATION_STATE_COMPLETED_WITH_ERRORS))
            progressStatus = SeadroidApplication.getAppContext().getString(R.string.notification_download_completed);
        else if (getState().equals(NotificationState.NOTIFICATION_STATE_COMPLETED))
            progressStatus = SeadroidApplication.getAppContext().getString(R.string.notification_download_completed);
        else if (getState().equals(NotificationState.NOTIFICATION_STATE_PROGRESS)) {
            int downloadingCount = 0;
            List<DownloadTaskInfo> infos = txService.getAllDownloadTaskInfos();
            for (DownloadTaskInfo info : infos) {
                if (info.state.equals(TaskState.INIT)
                        || info.state.equals(TaskState.TRANSFERRING))
                    downloadingCount++;
            }
            if (downloadingCount != 0)
                progressStatus = SeadroidApplication.getAppContext().getResources().
                        getQuantityString(R.plurals.notification_download_info,
                                downloadingCount,
                                downloadingCount,
                                getProgress());
        }
        return progressStatus;
    }

    @Override
    protected NotificationState getState() {
        if (txService == null)
            return NotificationState.NOTIFICATION_STATE_COMPLETED;

        List<DownloadTaskInfo> infos = txService.getAllDownloadTaskInfos();

        int progressCount = 0;
        int errorCount = 0;

        for (DownloadTaskInfo info : infos) {
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
        return NOTIFICATION_ID_DOWNLOAD;
    }

    @Override
    protected String getNotificationTitle() {
        return SeadroidApplication.getAppContext().getString(R.string.notification_download_started_title);
    }

    @Override
    public void notifyStarted() {
        Intent dIntent = new Intent(SeadroidApplication.getAppContext(), TransferActivity.class);
        dIntent.putExtra(NOTIFICATION_MESSAGE_KEY, NOTIFICATION_OPEN_DOWNLOAD_TAB);
        dIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent dPendingIntent = PendingIntent.getActivity(SeadroidApplication.getAppContext(),
                (int) System.currentTimeMillis(),
                dIntent,
                PendingIntent.FLAG_IMMUTABLE);
        mNotifBuilder = CustomNotificationBuilder.getNotificationBuilder(SeadroidApplication.getAppContext(),
                CustomNotificationBuilder.CHANNEL_ID_DOWNLOAD)
                .setSmallIcon(R.drawable.icon)
                .setOnlyAlertOnce(true)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentTitle(SeadroidApplication.getAppContext().getString(R.string.notification_download_started_title))
                .setOngoing(true)
                .setContentText(SeadroidApplication.getAppContext().getString(R.string.notification_download_started_title))
                .setContentIntent(dPendingIntent)
                .setProgress(100, 0, false);

        // Make this service run in the foreground, supplying the ongoing
        // notification to be shown to the user while in this state.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            txService.startForeground(NOTIFICATION_ID_DOWNLOAD, mNotifBuilder.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            txService.startForeground(NOTIFICATION_ID_DOWNLOAD, mNotifBuilder.build());
        }
    }

    @Override
    protected int getProgress() {
        long downloadedSize = 0l;
        long totalSize = 0l;
        if (txService == null)
            return 0;

        List<DownloadTaskInfo> infos = txService.getAllDownloadTaskInfos();
        for (DownloadTaskInfo info : infos) {
            if (info == null)
                continue;
            downloadedSize += info.finished;
            totalSize += info.fileSize;
        }

        // avoid ArithmeticException
        if (totalSize == 0l)
            return 0;
        return (int) (downloadedSize * 100 / totalSize);
    }

}