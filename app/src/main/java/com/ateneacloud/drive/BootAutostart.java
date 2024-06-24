package com.ateneacloud.drive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ateneacloud.drive.sync.clock.SyncWokerManager;

/**
 * This receiver is called whenever the system has booted or
 * the Seadroid app has been upgraded to a new version.
 * It can be used to start up background services.
 */
public class BootAutostart extends BroadcastReceiver {
    private static final String DEBUG_TAG = "BootAutostart";

    /**
     * This method will be excecuted after
     * - booting the device
     * - upgrade of the Seadroid package
     */
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(DEBUG_TAG, "Creating worker");
            SyncWokerManager.createSyncWoker(context);
        }
    }
}


