package com.ateneacloud.drive.cameraupload;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.ateneacloud.drive.BuildConfig;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountManager;


/**
 * Camera Upload Manager.
 * <p/>
 * This class can be used by other parts of Seadroid to enable/configure the camera upload
 * service.
 */
public class CameraUploadManager {

    /**
     * The authority of the camera sync service
     */
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".cameraupload.provider";

    AccountManager accountManager;

    public CameraUploadManager(Context context) {
        accountManager = new AccountManager(context);
    }

    /**
     * Is camera upload enabled?
     *
     * @return true if camera upload is enabled.
     */
    public boolean isCameraUploadEnabled() {
        Account account = getCameraAccount();
        return account != null;
    }

    /**
     * Get the account that is currently the remote target for the camera upload
     *
     * @return the account if camera is enabled, null otherwise.
     */
    public Account getCameraAccount() {
        for (Account account : accountManager.getAccountList()) {
            int isSyncable = ContentResolver.getIsSyncable(account.getAndroidAccount(), AUTHORITY);
            if (isSyncable > 0)
                return account;
        }
        return null;
    }

    /**
     * Initiate a camera sync immediately.
     */
    public void performSync() {
        Account cameraAccount = getCameraAccount();
        if (cameraAccount != null)
            ContentResolver.requestSync(cameraAccount.getAndroidAccount(), AUTHORITY, Bundle.EMPTY);
    }

    /**
     * Initiate a camera sync immediately, upload all media files again.
     */
    public void performFullSync() {
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, true);
        Log.d(CameraUploadManager.class.getName(),"performFullSync()~");

        Account cameraAccount = getCameraAccount();
        if (cameraAccount != null)
            ContentResolver.requestSync(cameraAccount.getAndroidAccount(), AUTHORITY, b);
    }

    public void performFullSyncIfEnable() {
        if (!isCameraUploadEnabled()){
            return;
        }
        performFullSync();
    }

    /**
     * Change the account currently responsible for camera upload.
     *
     * @param account An account. must not be null.
     */
    public void setCameraAccount(Account account) {
        for (Account a : accountManager.getAccountList()) {
            if (a.equals(account)) {
                // enable camera upload on this account
                ContentResolver.setIsSyncable(a.getAndroidAccount(), AUTHORITY, 1);
                ContentResolver.setSyncAutomatically(a.getAndroidAccount(), AUTHORITY, true);
            } else {
                // disable on all the others
                ContentResolver.cancelSync(a.getAndroidAccount(), AUTHORITY);
                ContentResolver.setIsSyncable(a.getAndroidAccount(), AUTHORITY, 0);

            }
        }
    }

    /**
     * Disable camera upload.
     */
    public void disableCameraUpload() {
        for (Account account : accountManager.getAccountList()) {
            ContentResolver.cancelSync(account.getAndroidAccount(), AUTHORITY);
            ContentResolver.setIsSyncable(account.getAndroidAccount(), AUTHORITY, 0);
        }
    }
}
