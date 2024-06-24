package com.ateneacloud.drive.sync;

import android.content.Context;

import com.ateneacloud.drive.SeafConnection;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for managing Seafile synchronization tasks. This service loads synchronization settings,
 * performs synchronization, and waits for all sync tasks to complete before stopping.
 */
public class SeafSyncronizerService {

    private static final String DEBUG_TAG = "SeafSyncronizerService";
    private static SeafSyncronizer seafSyncronizer = null;
    private static SeafConnection connection = null;
    public static boolean running = false;
    private static Date initSync;

    /**
     * Loads synchronization settings and starts synchronization.
     */
    private void loadSettings() {
        try {
            seafSyncronizer.startSync();
            waitSync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Waits for all synchronization tasks to complete.
     */
    private void waitSync() {
        boolean allFinish = false;
        waitSync:
        while (!allFinish) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            for (SeafSyncSettings syncSettings : seafSyncronizer.settings) {

                if (syncSettings.getLastExecution() == null) {
                    continue waitSync;
                } else if (syncSettings.getLastExecution().before(initSync)) {
                    continue waitSync;
                }

            }

            allFinish = true;
        }
        running = false;
    }


    /**
     * Starts the synchronization service.
     *
     * @param context The application context.
     */
    public void start(Context context) {
        if (!running) {
            initSync = new Date();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                seafSyncronizer = new SeafSyncronizer();
                loadSettings();

            });
            running = true;
        }
    }


    /**
     * Stops the synchronization service.
     */
    public void stop() {
        seafSyncronizer.stopSync();
        running = false;
    }
}
