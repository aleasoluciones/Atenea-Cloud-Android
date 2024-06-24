package com.ateneacloud.drive.sync.observers.impl;

import android.net.Uri;
import android.util.Log;

import com.ateneacloud.drive.sync.observers.SeafSyncObserverProtocol;
import com.ateneacloud.drive.sync.observers.callbacks.SeafSyncObserverProtocolCallback;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.tree.SeafSyncTree;
import com.ateneacloud.drive.sync.tree.services.SeafSyncTreeBuilder;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors changes in a Seafile Sync Folder specified by the provided SeafSyncSettings.
 * This observer periodically checks for changes in the folder's content and notifies the callback
 * when changes are detected.
 */
public class SeafSyncFolderObserver implements SeafSyncObserverProtocol {

    private String DEBUG_TAG = "SeafSyncSettingFolderObserver";
    private String identifier;
    private boolean running;
    private SeafSyncTree tree, treeOld;
    private SeafSyncTreeBuilder builder;

    /**
     * Constructs a new SeafSyncSettingFolderObserver for the specified SeafSyncSettings.
     *
     * @param setting The SeafSyncSettings defining the sync folder to observe.
     */
    public SeafSyncFolderObserver(SeafSyncSettings setting) {
        identifier = UUID.randomUUID().toString();
        running = false;
        builder = new SeafSyncTreeBuilder(setting.getResourceUri(), setting);
        tree = builder.build();
        treeOld = tree;
    }

    public SeafSyncFolderObserver(SeafSyncSettings setting, String path) {
        Uri uri = Uri.fromFile(new File(path));
        identifier = UUID.randomUUID().toString();
        running = false;
        builder = new SeafSyncTreeBuilder(uri, setting);
        tree = builder.build();
        treeOld = tree;
    }

    @Override
    public String getObserverIdentifier() {
        return identifier;
    }

    /**
     * Starts the observer and periodically checks for changes in the sync folder's content.
     * When changes are detected, the callback is notified.
     *
     * @param callback The callback to be notified when changes are detected.
     */
    @Override
    public void start(SeafSyncObserverProtocolCallback callback) {
        if (!running) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {
                running = true;
                while (running) {

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    tree = builder.build();
                    //Log.d(DEBUG_TAG, "Hahs: new " + tree.getFullHash() + " - old " + treeOld.getFullHash());
                    if (!tree.getFullHash().equals(treeOld.getFullHash())) {
                        Log.d(DEBUG_TAG, "There are changes: " + tree.getUri().getPath().toString());
                        if (callback != null) {
                            callback.onChanged();
                        }
                        treeOld = tree;
                    }else {
                        Log.d(DEBUG_TAG, "No change");
                    }
                }
            });
        }
    }

    /**
     * Stops the observer and ends the monitoring of the sync folder.
     */
    @Override
    public void stop() {
        running = false;
    }

}
