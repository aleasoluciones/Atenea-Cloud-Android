package com.ateneacloud.drive.sync.observers.impl;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.sync.fileProvider.providers.SeafSyncGalleryProvider;
import com.ateneacloud.drive.sync.observers.SeafSyncObserverProtocol;
import com.ateneacloud.drive.sync.observers.callbacks.SeafSyncObserverProtocolCallback;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SeafSyncGalleryObserver implements SeafSyncObserverProtocol {

    private static final String DEBUG_TAG = "SeafSyncGalleryObserver";
    private final Context context;
    private String identifier;
    private long lastCheckedMediaId = -1;
    private List<SeafSyncFolderObserver> observers;
    private SeafSyncSettings settings;
    private SeafSyncGalleryProvider galleryProvider;

    public SeafSyncGalleryObserver(SeafSyncSettings settings) {
        this.settings = settings;
        identifier = UUID.randomUUID().toString();
        context = SeadroidApplication.getAppContext();
        observers = new ArrayList<>();
    }

    @Override
    public String getObserverIdentifier() {
        return identifier;
    }

    @Override
    public void start(SeafSyncObserverProtocolCallback callback) {

        if (settings.getResourceUri() == null) {
            galleryProvider = new SeafSyncGalleryProvider();

            for (String id : galleryProvider.getAlbumsId()) {
                String path = getDirectoryForAlbumId(id);
                generatedObserver(path);
            }

        } else {
            galleryProvider = new SeafSyncGalleryProvider(settings.getResourceUri());
            generatedObserver(getDirectoryForAlbumId(galleryProvider.getAlbumIdFromUri(settings.getResourceUri())));
        }

        allStart(callback);

    }

    @Override
    public void stop() {
        allStop();
        observers.clear();
    }

    private void generatedObserver(String path) {
        SeafSyncFolderObserver folderObserver = new SeafSyncFolderObserver(settings, path);
        observers.add(folderObserver);
    }

    private void allStart(SeafSyncObserverProtocolCallback callback) {
        for (SeafSyncFolderObserver folderObserver : observers) {
            folderObserver.start(callback);
        }
    }

    private void allStop() {
        for (SeafSyncFolderObserver folderObserver : observers) {
            folderObserver.stop();
        }
    }

    private String getDirectoryForAlbumId(String albumId) {
        String directory = null;

        String[] projection = {MediaStore.Images.Media.DATA};
        String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        String[] selectionArgs = {String.valueOf(albumId)};
        String sortOrder = null;

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                directory = cursor.getString(columnIndex);

                File file = new File(directory);
                directory = file.getParent();
            }
            cursor.close();
        }

        return directory;
    }

}

