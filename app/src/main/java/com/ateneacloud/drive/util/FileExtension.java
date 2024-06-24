package com.ateneacloud.drive.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;

public class FileExtension {

    public static String getExtensionFromUri(Context context, Uri uri) {

        String fileExtension;

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            fileExtension = getExtensionFromUriPath(Uri.fromFile(new File(Utils.getPathFromUri(context, uri))));
        } else {
            fileExtension = getExtensionFromUriPath(uri);
        }

        return fileExtension.toUpperCase();
    }

    private static String getExtensionFromUriPath(Uri uri) {
        String fileExtension = "";
        String uriPath = uri.getPath();

        if (uriPath != null) {
            int lastDotIndex = uriPath.lastIndexOf('.');
            if (lastDotIndex != -1 && lastDotIndex < uriPath.length() - 1 && lastDotIndex != 0) {
                fileExtension = uriPath.substring(lastDotIndex + 1);
            }
        }

        return fileExtension;
    }

}
