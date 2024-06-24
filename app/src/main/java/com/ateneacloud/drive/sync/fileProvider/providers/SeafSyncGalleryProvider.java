package com.ateneacloud.drive.sync.fileProvider.providers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.sync.fileProvider.SeafSyncProviderProtocol;
import com.ateneacloud.drive.sync.fileProvider.syncItems.SeafSyncFileItem;
import com.ateneacloud.drive.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides access to gallery content for synchronization with Seafile.
 */
public class SeafSyncGalleryProvider implements SeafSyncProviderProtocol {
    private final String DEBUG_TAG = "SeafSyncGalleryProvider";
    private String albumId;
    private Context context;

    /**
     * Constructs a SeafSyncGalleryProvider instance for a specific album.
     *
     * @param album The URI of the album to synchronize.
     */
    public SeafSyncGalleryProvider(Uri album) {
        context = SeadroidApplication.getAppContext();
        this.albumId = getAlbumIdFromUri(album);
    }

    /**
     * Constructs a SeafSyncGalleryProvider instance for the entire gallery.
     */
    public SeafSyncGalleryProvider() {
        context = SeadroidApplication.getAppContext();
    }

    /**
     * Retrieves a list of files for synchronization from the gallery.
     *
     * @return A list of SeafSyncFileItem objects representing files in the gallery.
     */
    @Override
    public List<SeafSyncFileItem> getFiles() {
        if (albumId != null) {
            return listPhotosAndVideosToAlbum(albumId);
        }
        return listGallery();
    }

    /**
     * Retrieves a combined list of image and video files from all albums in the device's gallery.
     *
     * @return A list of SeafSyncFileItem objects representing both image and video files from all albums.
     */
    private List<SeafSyncFileItem> listGallery() {
        List<SeafSyncFileItem> photosAndVideos = new ArrayList<>();
        List<String> albumsId = getAlbumsId();

        for (String albumId : albumsId) {
            try {
                photosAndVideos.addAll(listPhotosToAlbum(albumId));
                photosAndVideos.addAll(listVideosToAlbum(albumId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return photosAndVideos;
    }

    /**
     * Retrieves a list of unique album IDs from the device's media content.
     *
     * @return A list of unique album IDs for organizing images and videos.
     */
    public List<String> getAlbumsId() {
        List<String> albumsId = new ArrayList<>();

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        };

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String albumId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID));
                //String albumName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));

                if (!albumsId.contains(albumId)) {
                    albumsId.add(albumId);
                }

            }
            cursor.close();
        }

        return albumsId;
    }

    /**
     * Retrieves a combined list of image and video files from the specified album.
     *
     * @param albumId The ID of the album to retrieve image and video files from.
     * @return A list of SeafSyncFileItem objects representing both image and video files.
     */
    private List<SeafSyncFileItem> listPhotosAndVideosToAlbum(String albumId) {
        List<SeafSyncFileItem> photosAndVideos = new ArrayList<>();

        try {

            photosAndVideos.addAll(listPhotosToAlbum(albumId));
            photosAndVideos.addAll(listVideosToAlbum(albumId));

        } catch (Exception e) {
            e.printStackTrace();
        }


        return photosAndVideos;

    }

    /**
     * Retrieves a list of image files from the specified album.
     *
     * @param albumID The ID of the album to retrieve images from.
     * @return A list of SeafSyncFileItem objects representing image files.
     */
    private List<SeafSyncFileItem> listPhotosToAlbum(String albumID) {
        List<SeafSyncFileItem> photos = new ArrayList<>();
        // Define the projection to obtain the album path
        String[] projection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection = new String[]{
                    MediaStore.Images.Media.RELATIVE_PATH,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media._ID
            };
        } else {
            projection = new String[]{
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME
            };
        }

        // Build the SQL query
        String selection = MediaStore.Images.Media.BUCKET_ID + "=?";
        String[] selectionArgs = {String.valueOf(albumID)};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            photos = retrieveCursorPhotos(cursor);
        }

        return photos;
    }

    /**
     * Retrieves a list of video files from the specified album.
     *
     * @param albumID The ID of the album to retrieve videos from.
     * @return A list of SeafSyncFileItem objects representing video files.
     */
    private List<SeafSyncFileItem> listVideosToAlbum(String albumID) {
        List<SeafSyncFileItem> videos = new ArrayList<>();
        // Define the projection to obtain the album path

        String[] projection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection = new String[]{
                    MediaStore.Video.Media.RELATIVE_PATH,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media._ID
            };
        } else {
            projection = new String[]{
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.DISPLAY_NAME
            };
        }

        // Build the SQL query
        String selection = MediaStore.Video.Media.BUCKET_ID + "=?";
        String[] selectionArgs = {String.valueOf(albumID)};
        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            videos = retrieveCursorVideos(cursor);
        }

        return videos;
    }


    /**
     * Retrieves a list of video files from a Cursor.
     *
     * @param cursor The Cursor containing video file information.
     * @return A list of SeafSyncFileItem objects representing video files.
     */
    private List<SeafSyncFileItem> retrieveCursorPhotos(Cursor cursor) {
        List<SeafSyncFileItem> photos = new ArrayList<>();

        while (cursor.moveToNext()) {
            String imagePath;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String relativePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH));
                String imageName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                imagePath = Utils.getPathFromUri(SeadroidApplication.getAppContext(), imageUri);
            } else {
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
            }

            if (imagePath != null && !imagePath.isEmpty()) {
                SeafSyncFileItem item = new SeafSyncFileItem(new File(imagePath));
                if (item.getIdentifier() != null && item.getCreationDate() != null && item.getModificationDate() != null) {
                    photos.add(item);
                }
            }

        }

        cursor.close();
        return photos;
    }

    /**
     * Retrieves a list of video files from a Cursor.
     *
     * @param cursor The Cursor containing video file information.
     * @return A list of SeafSyncFileItem objects representing video files.
     */
    private List<SeafSyncFileItem> retrieveCursorVideos(Cursor cursor) {
        List<SeafSyncFileItem> videos = new ArrayList<>();

        while (cursor.moveToNext()) {
            String videoPath;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String relativePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH));
                String videoName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                Uri videoUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                videoPath = Utils.getPathFromUri(SeadroidApplication.getAppContext(), videoUri);
            } else {
                videoPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
            }

            if (videoPath != null && !videoPath.isEmpty()) {
                SeafSyncFileItem item = new SeafSyncFileItem(new File(videoPath));
                if (item.getIdentifier() != null && item.getCreationDate() != null && item.getModificationDate() != null) {
                    videos.add(item);
                }
            }
        }

        cursor.close();

        return videos;
    }


    /**
     * Retrieves the album ID from a given URI.
     *
     * @param album The URI of the album.
     * @return The album ID as a String.
     */
    public String getAlbumIdFromUri(Uri album) {
        String albumId = getAlbumIdFromUriImage(album);

        if (albumId.isEmpty()) {
            albumId = getAlbumIdFromUriVideo(album);
        }

        return albumId;
    }

    private String getAlbumIdFromUriImage(Uri album) {
        String[] projection = {MediaStore.Images.Media.BUCKET_ID};
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Images.Media.DATA + " like ?",
                new String[]{album.getPath() + "%"},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
            String albumId = cursor.getString(columnIndex);
            cursor.close();
            return albumId;
        }
        return "";
    }

    private String getAlbumIdFromUriVideo(Uri album) {
        String[] projection = {MediaStore.Video.Media.BUCKET_ID};
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Video.Media.DATA + " like ?",
                new String[]{album.getPath() + "%"},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID);
            String albumId = cursor.getString(columnIndex);
            cursor.close();
            return albumId;
        }
        return "";
    }

    public String getAlbumPath(String albumId) {
        String path = getImagePath(albumId);

        if (path.isEmpty()) {
            path = getVideoPath(albumId);
        }

        return path;
    }

    private String getImagePath(String albumId) {
        String path = "";

        String[] projection = {MediaStore.Images.Media.DATA};

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Images.Media.BUCKET_ID + "=?";
        String[] selectionArgs = {albumId};

        Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            path = cursor.getString(columnIndex);
            cursor.close();
            File f = new File(path);
            path = f.getParent();
        }

        return path;
    }

    private String getVideoPath(String albumId) {
        String path = "";

        String[] projection = {MediaStore.Video.Media.DATA};

        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Video.Media.BUCKET_ID + "=?";
        String[] selectionArgs = {albumId};

        Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
            path = cursor.getString(columnIndex);
            cursor.close();
            File f = new File(path);
            path = f.getParent();
        }

        return path;
    }
}