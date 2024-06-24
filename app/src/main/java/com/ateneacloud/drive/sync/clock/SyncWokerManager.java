package com.ateneacloud.drive.sync.clock;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.ateneacloud.drive.SeadroidApplication;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages the scheduling and status checking of a periodic synchronization worker.
 */
public class SyncWokerManager {

    private static final SharedPreferences PREFERENCES = SeadroidApplication.getAppContext().getSharedPreferences("SeafileSyncPreferences", Context.MODE_PRIVATE);

    public static final int DEFAULT_VALUE = 2;

    private static final String DEBUG_TAG = "SyncWokerManager";

    /**
     * Creates and schedules a periodic synchronization worker if not already scheduled.
     *
     * @param context The Android application context.
     * @return True if the worker is created and scheduled, false if it was already scheduled.
     */
    public static boolean createSyncWoker(Context context) {

        if (!isSyncWorkerScheduled(context)) {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                    SyncWorker.class,
                    getTime(),
                    getTimeUnit()
            ).addTag("SEADROID_SYNC_WORKER").setConstraints(constraints).build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "SEADROID_SYNC_WORKER",
                    ExistingPeriodicWorkPolicy.REPLACE, // Opciones para manejar tareas duplicadas
                    syncRequest
            );
            return true;
        }

        return false;

    }

    /**
     * Creates and schedules a periodic synchronization worker if not already scheduled.
     *
     * @param context The Android application context.
     * @return True if the worker is created and scheduled, false if it was already scheduled.
     */
    public static boolean deleteSyncWoker(Context context) {

        if (isSyncWorkerScheduled(context)) {
            WorkManager.getInstance(context).cancelAllWorkByTag("SEADROID_SYNC_WORKER");
            return true;
        }

        return false;

    }

    /**
     * Checks if the synchronization worker is currently scheduled or running.
     *
     * @param context The Android application context.
     * @return True if the worker is scheduled or running, false otherwise.
     */
    public static boolean isSyncWorkerScheduled(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);

        ListenableFuture<List<WorkInfo>> statuses = workManager.getWorkInfosForUniqueWork("SEADROID_SYNC_WORKER");

        try {
            List<WorkInfo> workInfoList = statuses.get();
            for (WorkInfo workInfo : workInfoList) {
                if (workInfo.getState() == WorkInfo.State.ENQUEUED || workInfo.getState() == WorkInfo.State.RUNNING) {

                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static int getTime() {
        int time = PREFERENCES.getInt("syncTime", SyncWokerManager.DEFAULT_VALUE);
        time = time == 0 ? 30 : time;
        return time;
    }

    private static TimeUnit getTimeUnit() {
        int time = PREFERENCES.getInt("syncTime", SyncWokerManager.DEFAULT_VALUE);
        TimeUnit unite = time == 0 ? TimeUnit.MINUTES : TimeUnit.HOURS;
        return unite;
    }

}
