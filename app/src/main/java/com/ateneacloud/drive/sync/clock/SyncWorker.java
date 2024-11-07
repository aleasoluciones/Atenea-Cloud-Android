package com.ateneacloud.drive.sync.clock;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ateneacloud.drive.sync.SeafSyncronizerService;

/**
 * A worker for performing synchronization tasks in the background using Android WorkManager.
 */
public class SyncWorker extends Worker {

    private static final String CHANNEL_ID = "seadroid_application_sync_service";
    private static final int NOTIFICATION_ID = 61616262;
    private static final String DEBUG_TAG = "SyncWorker";

    private static boolean running = false;

    /**
     * Constructs a new SyncWorker.
     *
     * @param context      The application context.
     * @param workerParams The parameters for the worker.
     */
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

    }


    /**
     * Performs the synchronization work in the background.
     *
     * @return The result of the synchronization task (success or failure).
     */
    @NonNull
    @Override
    public Result doWork() {

        if (!running) {
            running = true;

            SeafSyncronizerService seafSyncronizerService = new SeafSyncronizerService();

//            createAndShowNotification();

            try {
                Log.d(DEBUG_TAG, "Starting synchronization worker");
                seafSyncronizerService.start(getApplicationContext());

                while (seafSyncronizerService.running) {
                    Log.d(DEBUG_TAG, "Checking for completion");
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                seafSyncronizerService.stop();
                Log.d(DEBUG_TAG, "Synchronization completed");
            } catch (Exception e) {
                e.printStackTrace();
            }

//            cancelNotification();

            running = false;
        }


        return Result.success();
    }
}