package com.ateneacloud.drive.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountInfo;
import com.ateneacloud.drive.account.AccountPlans;
import com.ateneacloud.drive.play.VideoLinkStateListener;
import com.ateneacloud.drive.play.VideoLinkTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ValidatesFiles {

    private static final String[] ADVANCE_FILE_TYPES = new String[]{"TIFF", "RAW", "PSD", "EPS", "AI", "SVG", "FLV", "WMV"};
    private static final String[] VIDEO_EXTENSIONS = new String[]{"MP4", "MKV", "AVI", "MOV", "WMV"};

    private static final int MAX_SIZE_UPLOAD_AND_DOWNLOAD = 1024 * 1024 * 1024;

    public static ArrayList<Uri> getNotValidFiles(Activity activity, AccountInfo accountInfo, List<Uri> uris) {
        ArrayList<Uri> notValidUris = new ArrayList<>();

        for (Uri uri : uris) {
            if (!isValidFile(activity, accountInfo, uri)) {
                notValidUris.add(uri);
            }
        }

        return notValidUris;
    }

    public static boolean isValidFile(Activity activity, AccountInfo accountInfo, Uri uri) {

        String type = FileExtension.getExtensionFromUri(activity.getApplicationContext(), uri);

        if (!isValidType(accountInfo, type.toUpperCase())) {
            return false;
        } else if (accountInfo.getPlan() != AccountPlans.Platinum) {
            long fileSize = getFileSize(activity.getApplicationContext(), uri);

            if (Arrays.stream(VIDEO_EXTENSIONS).anyMatch(type::equals)) {
                if (is4KVideoByUri(activity, uri)) return false;
            }

            if (fileSize >= MAX_SIZE_UPLOAD_AND_DOWNLOAD || fileSize == -1) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidFileCloud(
            Account account,
            AccountInfo accountInfo,
            String fileName,
            String filePath,
            long fileSize,
            String repoID) {


        String fileExtention = "";
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            fileExtention = fileName.substring(lastDotIndex + 1).toUpperCase();
        }

        if (!isValidType(accountInfo, fileExtention)) {
            return false;
        }

        if (accountInfo.getPlan() != AccountPlans.Platinum) {

            if (Arrays.stream(VIDEO_EXTENSIONS).anyMatch(fileExtention::equals)) {
                String url = getVideoLink(account, repoID, filePath);
                return !url.isEmpty() && !is4KVideoByUrl(SeadroidApplication.getAppContext(), url);
            }

            if (fileSize > MAX_SIZE_UPLOAD_AND_DOWNLOAD || fileSize == -1) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidFile(Context context, AccountInfo accountInfo, Uri uri) {
        String type = FileExtension.getExtensionFromUri(context, uri);
        if ((accountInfo.getPlan() != AccountPlans.Enterprise && accountInfo.getPlan() != AccountPlans.Platinum) && Arrays.stream(ADVANCE_FILE_TYPES).anyMatch(type::equals)) {
            return false;
        } else if (accountInfo.getPlan() != AccountPlans.Platinum) {
            long fileSize = getFileSize(context, uri);
            if (Arrays.stream(VIDEO_EXTENSIONS).anyMatch(type::equals)) {
                if (is4KVideoByUri(context, uri)) return false;
            } else if (fileSize > MAX_SIZE_UPLOAD_AND_DOWNLOAD || fileSize == -1) {
                return false;
            }
        }


        return true;
    }

    private static String getVideoLink(Account account, String mRepoID, String mFilePath) {
        final String[] url = {null};
        VideoLinkTask task = new VideoLinkTask(account, mRepoID, mFilePath, new VideoLinkStateListener() {
            @Override
            public void onSuccess(String fileLink) {
                url[0] = fileLink;
            }

            @Override
            public void onError(String errMsg) {
                url[0] = "";
            }
        });

        ConcurrentAsyncTask.execute(task);

        while (url[0] == null) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return url[0];
    }

    private static long getFileSize(Context context, Uri uri) {

        String path = uri.getPath();
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                return file.length();
            } else {
                path = Utils.getPathFromUri(context, uri);
                file = new File(path);
                if (file.exists()) {
                    return file.length();
                }
            }
        }

        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1) {
                return cursor.getLong(sizeIndex);
            }
        }

        return -1;
    }


    public static boolean is4KVideoByUri(Context context, Uri videoUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, videoUri);
        return is4KVideo(retriever);

    }

    public static boolean is4KVideoByUrl(Context context, String videoUrl) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoUrl, new HashMap<String, String>());
            return is4KVideo(retriever);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }

    private static boolean is4KVideo(MediaMetadataRetriever retriever) {
        String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        if (width != null && height != null) {
            int videoWidth = Integer.parseInt(width);
            int videoHeight = Integer.parseInt(height);
            if (videoWidth >= 3840 && videoHeight >= 2160) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVideoByFile(File video) {
        ContentResolver contentResolver = SeadroidApplication.getAppContext().getContentResolver();
        Uri uri = Uri.fromFile(video);
        String tipoMIME = contentResolver.getType(uri);
        if (tipoMIME != null && tipoMIME.startsWith("video/")) {
            return true;
        }
        return false;
    }

    public static boolean isValidType(AccountInfo accountInfo, String extension) {

        if ((accountInfo.getPlan() != AccountPlans.Enterprise && accountInfo.getPlan() != AccountPlans.Platinum)
                && Arrays.stream(ADVANCE_FILE_TYPES).anyMatch(extension::equals)) {
            return false;
        }

        return true;
    }

}