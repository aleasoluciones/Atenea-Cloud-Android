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

//    private void createAndShowNotification() {
//        // Obtener el servicio de notificación
//        NotificationManager notificationManager = getSystemService(getApplicationContext(), NotificationManager.class);
//
//        // Crear un canal de notificación (requerido para Android 8.0 y superior)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Seafile Sync", NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // Crear un intent para abrir la actividad cuando se toque la notificación
//        Intent intent = new Intent(getApplicationContext(), BrowserActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        // Crear la notificación
//        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
//                .setContentTitle("Sincronización en progreso")
//                .setContentText("La sincronización está en curso")
//                .setSmallIcon(R.drawable.icon)
//                .setContentIntent(pendingIntent)
//                .build();
//
//        // Mostrar la notificación
//        notificationManager.notify(NOTIFICATION_ID, notification);
//    }
//
//    private void cancelNotification() {
//        NotificationManager notificationManager = getSystemService(getApplicationContext(), NotificationManager.class);
//        notificationManager.cancel(NOTIFICATION_ID);
//    }
}