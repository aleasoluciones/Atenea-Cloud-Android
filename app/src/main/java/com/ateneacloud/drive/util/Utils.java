package com.ateneacloud.drive.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.LocaleList;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.SettingsManager;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.cameraupload.CameraUploadDBHelper;
import com.ateneacloud.drive.cameraupload.CameraUploadManager;
import com.ateneacloud.drive.cameraupload.GalleryBucketUtils;
import com.ateneacloud.drive.data.SeafRepo;
import com.ateneacloud.drive.fileschooser.SelectableFile;
import com.ateneacloud.drive.folderbackup.FolderBackupDBHelper;
import com.ateneacloud.drive.folderbackup.FolderBackupInfo;
import com.ateneacloud.drive.folderbackup.selectfolder.StringTools;
import com.ateneacloud.drive.sync.enums.SeafSyncMode;
import com.ateneacloud.drive.sync.enums.SeafSyncNetwork;
import com.ateneacloud.drive.sync.enums.SeafSyncType;
import com.ateneacloud.drive.sync.fileProvider.providers.SeafSyncGalleryProvider;
import com.ateneacloud.drive.sync.fileProvider.syncItems.SeafSyncFileItem;
import com.ateneacloud.drive.sync.logs.services.SeafSyncLogsService;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.settings.SeafSyncSettingsService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Utils {
    public static final String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String AUTHORITY = "com.ateneacloud.drive";
    public static final String PATH_SEPERATOR = "/";
    // public static final String NOGROUP = "$nogroup";
    public static final String PERSONAL_REPO = "personal_repo";
    public static final String SHARED_REPO = "shared_repo";
    public static final String TRANSFER_PHOTO_TAG = "camera_upload";
    public static final String TRANSFER_FOLDER_TAG = "folder_backup";
    private static final String DEBUG_TAG = "Utils";
    private static final String HIDDEN_PREFIX = ".";
    private static final String DOCUMENTS_DIR = "documents";
    private static HashMap<String, Integer> suffixIconMap = null;
    private static final int JOB_ID = 0;

    private Context context;
    private Utils() {
    }

    public static JSONObject parseJsonObject(String json) {
        if (json == null) {
            // the caller should not give null
            Log.w(DEBUG_TAG, "null in parseJsonObject");
            return null;
        }

        try {
            return (JSONObject) new JSONTokener(json).nextValue();
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONArray parseJsonArrayByKey(@NonNull String json, @NonNull String key) throws JSONException {
        String value = new JSONObject(json).optString(key);
        if (!TextUtils.isEmpty(value)) return parseJsonArray(value);
        else return null;
    }

    public static JSONArray parseJsonArray(@NonNull String json) {
        try {
            return (JSONArray) new JSONTokener(json).nextValue();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Could not parse json file", e);
            return null;
        }
    }

    public static String readFile(File file) {
        Reader reader = null;
        try {
            try {
                // TODO: detect a file's encoding
                reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }

            char[] buffer = new char[1024];
            StringBuilder responseStrBuilder = new StringBuilder();

            while (true) {
                int len = reader.read(buffer, 0, 1024);
                if (len == -1) break;
                responseStrBuilder.append(buffer, 0, len);
            }
            return responseStrBuilder.toString();
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {

            }
        }
    }

    public static String getParentPath(String path) {
        if (path == null) {
            // the caller should not give null
            Log.w(DEBUG_TAG, "null in getParentPath");
            return null;
        }

        if (!path.contains("/")) {
            return "/";
        }

        String parent = path.substring(0, path.lastIndexOf("/"));
        if (parent.equals("")) {
            return "/";
        } else return parent;
    }

    public static String fileNameFromPath(String path) {
        if (path == null) {
            // the caller should not give null
            Log.w(DEBUG_TAG, "null in getParentPath");
            return null;
        }

        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0 KB";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1000));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1000, digitGroups)) + " " + units[digitGroups];
    }

    public static void writeFile(File file, String content) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(content.getBytes("UTF-8"));
        } finally {
            try {
                if (os != null) os.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public static TreeMap<String, List<SeafRepo>> groupRepos(List<SeafRepo> repos) {
        TreeMap<String, List<SeafRepo>> map = new TreeMap<String, List<SeafRepo>>();
        String groupName = null;
        for (SeafRepo repo : repos) {
            List<SeafRepo> l;
            if (repo.isGroupRepo) groupName = repo.owner;
            else if (repo.isPersonalRepo) groupName = PERSONAL_REPO;
            else if (repo.isSharedRepo) groupName = SHARED_REPO;

            l = map.get(groupName);
            if (l == null) {
                l = Lists.newArrayList();
                map.put(groupName, l);
            }
            l.add(repo);
        }
        return map;
    }

    public static int getResIdforMimetype(String mimetype) {
        if (mimetype == null) return R.drawable.file;

        if (mimetype.contains("pdf")) {
            return R.drawable.file_pdf;
        } else if (mimetype.contains("image/")) {
            return R.drawable.file_image;
        } else if (mimetype.contains("text")) {
            return R.drawable.file_text;
        } else if (mimetype.contains("audio")) {
            return R.drawable.file_audio;
        } else if (mimetype.contains("video")) {
            return R.drawable.file_video;
        }
        if (mimetype.contains("pdf")) {
            return R.drawable.file_pdf;
        } else if (mimetype.contains("msword") || mimetype.contains("ms-word")) {
            return R.drawable.file_ms_word;
        } else if (mimetype.contains("mspowerpoint") || mimetype.contains("ms-powerpoint")) {
            return R.drawable.file_ms_ppt;
        } else if (mimetype.contains("msexcel") || mimetype.contains("ms-excel")) {
            return R.drawable.file_ms_excel;
        } else if (mimetype.contains("openxmlformats-officedocument")) {
            // see http://stackoverflow.com/questions/4212861/what-is-a-correct-mime-type-for-docx-pptx-etc
            if (mimetype.contains("wordprocessingml")) {
                return R.drawable.file_ms_word;
            } else if (mimetype.contains("spreadsheetml")) {
                return R.drawable.file_ms_excel;
            } else if (mimetype.contains("presentationml")) {
                return R.drawable.file_ms_ppt;
            }
            // } else if (mimetype.contains("application")) {
            //     return R.drawable.file_binary;
        }

        return R.drawable.file;
    }

    private static synchronized HashMap<String, Integer> getSuffixIconMap() {
        if (suffixIconMap != null) return suffixIconMap;

        suffixIconMap = Maps.newHashMap();
        suffixIconMap.put("pdf", R.drawable.file_pdf);
        suffixIconMap.put("doc", R.drawable.file_ms_word);
        suffixIconMap.put("docx", R.drawable.file_ms_word);
        suffixIconMap.put("md", R.drawable.file_text);
        suffixIconMap.put("markdown", R.drawable.file_text);
        return suffixIconMap;
    }

    public static int getFileIcon(String name) {
        String suffix = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (suffix.length() == 0) {
            return R.drawable.file;
        }

        HashMap<String, Integer> map = getSuffixIconMap();
        Integer i = map.get(suffix);
        if (i != null) return i;

        if (suffix.equals("flv")) {
            return R.drawable.file_video;
        }
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        return getResIdforMimetype(mime);
    }

    public static int getFileIconSuffix(String suffix) {
        if (suffix.length() == 0) {
            return R.drawable.file;
        }

        HashMap<String, Integer> map = getSuffixIconMap();
        Integer i = map.get(suffix);
        if (i != null) return i;

        if (suffix.equals("flv")) {
            return R.drawable.file_video;
        }
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        return getResIdforMimetype(mime);
    }

    public static boolean isViewableImage(String name) {
        String suffix = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (suffix.length() == 0) return false;
        if (suffix.equals("svg"))
            // don't support svg preview
            return false;

        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (mime == null) return false;
        return mime.contains("image/");
    }

    public static boolean isVideoFile(String name) {
        if (name == null) return false;
        String suffix = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (TextUtils.isEmpty(suffix)) return false;
        if (suffix.equals("flv")) {
            return true;
        }
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (mime == null) return false;
        return mime.contains("video/");
    }

    public static boolean isTextFile(File file) {
        if (file != null) {
            String fileName = file.getName();
            if (!TextUtils.isEmpty(fileName)) {
                String suffix = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                if (!TextUtils.isEmpty(suffix)) {
                    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
                    if (!TextUtils.isEmpty(mime)) {
                        return mime.contains("text/") || FileMimeUtils.isOfficeOrTextFile(mime);
                    }
                }
            }
        }
        return false;
    }

    public static boolean isNetworkOn() {
        ConnectivityManager connMgr = (ConnectivityManager) SeadroidApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }
        if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            String extraInfo = networkInfo.getExtraInfo();
            if (!TextUtils.isEmpty(extraInfo)) {
                return true;
            }
        }
        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    public static boolean isWiFiOn() {
        ConnectivityManager connMgr = (ConnectivityManager) SeadroidApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi != null && wifi.isAvailable() && wifi.getDetailedState() == DetailedState.CONNECTED) {
            return true;
        }
        return false;
    }

    public static String pathJoin(String first, String... rest) {
        StringBuilder result = new StringBuilder(first);
        for (String b : rest) {
            boolean resultEndsWithSlash = result.toString().endsWith("/");
            boolean bStartWithSlash = b.startsWith("/");
            if (resultEndsWithSlash && bStartWithSlash) {
                result.append(b.substring(1));
            } else if (resultEndsWithSlash || bStartWithSlash) {
                result.append(b);
            } else {
                result.append("/");
                result.append(b);
            }
        }

        return result.toString();
    }

    public static String removeLastPathSeperator(String path) {
        if (TextUtils.isEmpty(path)) return null;

        int size = path.length();
        if (path.endsWith("/")) {
            return path.substring(0, size - 1);
        } else return path;
    }

    /**
     * Strip leading and trailing slashes
     */
    public static String stripSlashes(String a) {
        return a.replaceAll("^[/]*|[/]*$", "");
    }

    public static String getCurrentHourMinute() {
        return (String) DateFormat.format("hh:mm", new Date());
    }

    /**
     * Translate commit time to human readable time description
     */
    public static String translateCommitTime(long timestampInMillis) {
        long now = Calendar.getInstance().getTimeInMillis();
        if (now <= timestampInMillis) {
            return SeadroidApplication.getAppContext().getString(R.string.just_now);
        }

        long delta = (now - timestampInMillis) / 1000;

        long secondsPerDay = 24 * 60 * 60;

        long days = delta / secondsPerDay;
        long seconds = delta % secondsPerDay;

        if (days >= 14) {
            Date d = new Date(timestampInMillis);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            return fmt.format(d);
        } else if (days > 0) {
            return SeadroidApplication.getAppContext().getString(R.string.days_ago, days);
        } else if (seconds >= 60 * 60) {
            long hours = seconds / 3600;
            return SeadroidApplication.getAppContext().getString(R.string.hours_ago, hours);
        } else if (seconds >= 60) {
            long minutes = seconds / 60;
            return SeadroidApplication.getAppContext().getString(R.string.minutes_ago, minutes);
        } else if (seconds > 0) {
            return SeadroidApplication.getAppContext().getString(R.string.seconds_ago, seconds);
        } else {
            return SeadroidApplication.getAppContext().getString(R.string.just_now);
        }
    }

    /**
     * Translate create time
     */
    public static String translateTime() {
        long now = Calendar.getInstance().getTimeInMillis();
        Date d = new Date(now);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        return fmt.format(d);
    }


    public static long now() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public static String getFileMimeType(String path) {
        String name = fileNameFromPath(path);
        String suffix = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (suffix.length() == 0) {
            return MIME_APPLICATION_OCTET_STREAM;
        } else {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
            if (mime != null) {
                return mime;
            } else {
                return MIME_APPLICATION_OCTET_STREAM;
            }
        }
    }

    public static String getFileMimeType(File file) {
        return getFileMimeType(file.getPath());
    }

    public static void copyFile(File src, File dst) throws IOException {
        if (src == null || dst == null) {
            return;
        }
        InputStream in = new BufferedInputStream(new FileInputStream(src));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(dst));

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /************ MutiFileChooser ************/
    private static Comparator<SelectableFile> mComparator = new Comparator<SelectableFile>() {
        public int compare(SelectableFile f1, SelectableFile f2) {
            // Sort alphabetically by lower case, which is much cleaner
            return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
        }
    };

    private static FileFilter mFileFilter = new FileFilter() {
        public boolean accept(File file) {
            final String fileName = file.getName();
            // Return files only (not directories) and skip hidden files
            return file.isFile() && !fileName.startsWith(HIDDEN_PREFIX);
        }
    };

    private static FileFilter mDirFilter = new FileFilter() {
        public boolean accept(File file) {
            final String fileName = file.getName();
            // Return directories only and skip hidden directories
            return file.isDirectory() && !fileName.startsWith(HIDDEN_PREFIX);
        }
    };

    public static List<SelectableFile> getFileList(String path, List<File> selectedFile) {
        ArrayList<SelectableFile> list = Lists.newArrayList();

        // Current directory File instance
        final SelectableFile pathDir = new SelectableFile(path);

        // List file in this directory with the directory filter
        final SelectableFile[] dirs = pathDir.listFiles(mDirFilter);
        if (dirs != null) {
            // Sort the folders alphabetically
            Arrays.sort(dirs, mComparator);
            // Add each folder to the File list for the list adapter
            for (SelectableFile dir : dirs) list.add(dir);
        }

        // List file in this directory with the file filter
        final SelectableFile[] files = pathDir.listFiles(mFileFilter);
        if (files != null) {
            // Sort the files alphabetically
            Arrays.sort(files, mComparator);
            // Add each file to the File list for the list adapter
            for (SelectableFile file : files) {
                if (selectedFile != null) {
                    if (selectedFile.contains(file.getFile())) {
                        file.setSelected(true);
                    }
                }
                list.add(file);
            }
        }

        return list;
    }

    public static Intent createGetContentIntent() {
        // Implicitly allow the user to select a particular kind of data
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // The MIME data type filter
        intent.setType("*/*");
        // Only return URIs that can be opened with ContentResolver
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Allow user to select multiple files
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            // only show local document providers
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        }
        return intent;
    }

    public static String getFilenamefromUri(Context context, Uri uri) {

        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null);
        String displayName = null;
        if (cursor != null && cursor.moveToFirst()) {

            // Note it's called "Display Name".  This is
            // provider-specific, and might not necessarily be the file name.
            displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            cursor.close();
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            displayName = uri.getPath().replaceAll(".*/", "");
        } else displayName = "unknown filename";
        return displayName;
    }

    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getStackTrace(Exception e) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        e.printStackTrace(writer);
        return buffer.toString();
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromStream(InputStream stream, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        // return BitmapFactory.decodeResource(res, resId, options);
        return BitmapFactory.decodeStream(stream, null, options);
    }

    public static String assembleUserName(String name, String email, String server) {
        if (name == null || email == null || server == null) return null;

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(server))
            return "";

        // strip port, like :8000 in 192.168.1.116:8000
        if (server.indexOf(":") != -1) server = server.substring(0, server.indexOf(':'));
//        String info = String.format("%s (%s)", email, server);//settingFragmeng set account name
        String info = String.format("%s (%s)", name, server);
        info = info.replaceAll("[^\\w\\d\\.@\\(\\) ]", "_");
        return info;
    }

    public static void hideSoftKeyboard(View view) {
        if (view == null) return;
        ((InputMethodManager) SeadroidApplication.getAppContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static String cleanServerURL(String serverURL) throws MalformedURLException {
        if (!serverURL.endsWith("/")) {
            serverURL = serverURL + "/";
        }

        // XXX: android 4.0.3 ~ 4.0.4 can't handle urls with underscore (_) in the host field.
        // See https://github.com/nostra13/Android-Universal-Image-Loader/issues/256 , and
        // https://code.google.com/p/android/issues/detail?id=24924
        //
        new URL(serverURL); // will throw MalformedURLException if serverURL not valid
        return serverURL;
    }

    public static ResolveInfo getWeChatIntent(Intent intent) {
        PackageManager pm = SeadroidApplication.getAppContext().getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info : infos) {
            if (info.activityInfo.packageName.equals("com.tencent.mm")) {
                return info;
            }
        }

        return null;
    }


    /**
     * use compare user system  is chinese
     */
    public static boolean isInChina() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = LocaleList.getDefault().get(0);
        } else {
            locale = Locale.getDefault();
        }
        String language = locale.getCountry();
        return TextUtils.equals("CN", language) || TextUtils.equals("TW", language);
    }

    public static List<ResolveInfo> getAppsByIntent(Intent intent) {
        PackageManager pm = SeadroidApplication.getAppContext().getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);

        // Remove seafile app from the list
        String seadroidPackageName = SeadroidApplication.getAppContext().getPackageName();
        ResolveInfo info;
        Iterator<ResolveInfo> iter = infos.iterator();
        while (iter.hasNext()) {
            info = iter.next();
            if (info.activityInfo.packageName.equals(seadroidPackageName)) {
                iter.remove();
            }
        }

        return infos;
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static boolean isTextMimeType(String fileName) {
        String suffix = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        //file is markdown or  txt
        String[] array = {"ac", "am", "bat", "c", "cc", "cmake", "conf", "cpp", "cs", "css", "csv", "diff", "el", "go", "groovy", "h", "htm", "html", "java", "js", "json", "less", "log", "make", "markdown", "md", "org", "patch", "pde", "php", "pl", "properties", "py", "rb", "rst", "sc", "scala", "scd", "schelp", "script", "sh", "sql", "text", "tex", "txt", "vi", "vim", "xhtml", "xml", "yml", "adoc"};
        boolean flag = Arrays.asList(array).contains(suffix);
        return flag;
    }

    private static long lastClickTime;

    /**
     * check if click event is a fast tapping
     *
     * @return
     */
    public static boolean isFastTapping() {
        long time = System.currentTimeMillis();
        if (time - lastClickTime < 500) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    /**
     * SslCertificate class does not has a public getter for the underlying
     * X509Certificate, we can only do this by hack. This only works for andorid 4.0+
     *
     * @see https://groups.google.com/forum/#!topic/android-developers/eAPJ6b7mrmg
     */
    public static X509Certificate getX509CertFromSslCertHack(SslCertificate sslCert) {
        X509Certificate x509Certificate = null;

        Bundle bundle = SslCertificate.saveState(sslCert);
        byte[] bytes = bundle.getByteArray("x509-certificate");

        if (bytes == null) {
            x509Certificate = null;
        } else {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                x509Certificate = (X509Certificate) cert;
            } catch (CertificateException e) {
                x509Certificate = null;
            }
        }

        return x509Certificate;
    }

    public static boolean isSameCert(SslCertificate sslCert, X509Certificate x509Cert) {
        if (sslCert == null || x509Cert == null) {
            return false;
        }

        X509Certificate realCert = getX509CertFromSslCertHack(sslCert);
        if (realCert != null) {
            // for android 4.0+
            return realCert.equals(x509Cert);
        } else {
            // for andorid < 4.0
            return SslCertificateComparator.compare(sslCert, new SslCertificate(x509Cert));
        }
    }

    /**
     * Compare SslCertificate objects for android before 4.0
     */
    public static class SslCertificateComparator {
        private SslCertificateComparator() {
        }

        public static boolean compare(SslCertificate cert1, SslCertificate cert2) {
            return isSameDN(cert1.getIssuedTo(), cert2.getIssuedTo()) && isSameDN(cert1.getIssuedBy(), cert2.getIssuedBy()) && isSameDate(cert1.getValidNotBeforeDate(), cert2.getValidNotBeforeDate()) && isSameDate(cert1.getValidNotAfterDate(), cert2.getValidNotAfterDate());
        }

        private static boolean isSameDate(Date date1, Date date2) {
            if (date1 == null && date2 == null) {
                return true;
            } else if (date1 == null || date2 == null) {
                return false;
            }

            return date1.equals(date2);
        }

        private static boolean isSameDN(SslCertificate.DName dName1, SslCertificate.DName dName2) {
            if (dName1 == null && dName2 == null) {
                return true;
            } else if (dName1 == null || dName2 == null) {
                return false;
            }

            return dName1.getDName().equals(dName2.getDName());
        }
    }

    public static int dip2px(Context context, float dip) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static void hideSystemNavigationBar(Activity activity) {
        if (activity == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < 19) {
            View view = activity.getWindow().getDecorView();
            view.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = activity.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    public static int getThumbnailWidth() {
        return (int) SeadroidApplication.getAppContext().getResources().getDimension(R.dimen.gallery_icon_show);
    }

    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (TextUtils.isEmpty(ServiceName)) {
            return false;
        }
        ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString().equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }

//    public static void startCameraSyncJob(Context context) {
//        JobScheduler mJobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
//        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, new ComponentName(context.getPackageName(), MediaSchedulerService.class.getName()));
//        builder.setMinimumLatency(5 * 1000);// Set to execute after at least 15 minutes delay
//        builder.setOverrideDeadline(60 * 60 * 1000);// The setting is delayed by 20 minutes,
//        builder.setRequiresCharging(false);
//        builder.setRequiresDeviceIdle(false);
//        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
//        builder.setPersisted(true);
//        mJobScheduler.schedule(builder.build());
//    }

    public static String getSyncCompletedTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm");
        Date date = new Date(System.currentTimeMillis());
        String completedTime = formatter.format(date);
        return completedTime;
    }

    public static String getUploadStateShow(Context context) {
        String results = null;
        int scanUploadStatus = SeadroidApplication.getInstance().getScanUploadStatus();
        int waitingNumber = SeadroidApplication.getInstance().getWaitingNumber();
        int totalNumber = SeadroidApplication.getInstance().getTotalNumber();
        switch (scanUploadStatus) {
            case CameraSyncStatus.SCANNING:
                results = context.getString(R.string.is_scanning);
                break;
            case CameraSyncStatus.NETWORK_UNAVAILABLE:
                results = context.getString(R.string.network_unavailable);
                break;
            case CameraSyncStatus.UPLOADING:
                results = context.getString(R.string.is_uploading) + " " + (totalNumber - waitingNumber) + " / " + totalNumber;
                break;
            case CameraSyncStatus.SCAN_END:
                results = context.getString(R.string.Upload_completed) + " " + SettingsManager.instance().getUploadCompletedTime();
                break;
            default:
                results = context.getString(R.string.folder_backup_waiting_state);
                break;
        }
        return results;
    }

    public static String toURLEncoded(String paramString) {
        if (paramString == null || paramString.equals("")) {
            return "";
        }
        try {
            String str = new String(paramString.getBytes(), "UTF-8");
            str = URLEncoder.encode(str, "UTF-8");
            return str;
        } catch (Exception localException) {
        }
        return "";
    }

    public static String getRealPathFromURI(Context context, Uri contentUri, String media) {
        Cursor cursor = null;
        try {
            if (media.equals("images")) {//image
                String[] proj = {MediaStore.Images.Media.DATA};
                cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } else {//Video
                String[] proj = {MediaStore.Video.Media.DATA};
                cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void logPhoneModelInfo() {
        SeafileLog.d(DEBUG_TAG, "phoneModelInfo-------" + SeafileLog.getDeviceBrand() + "/" + SeafileLog.getSystemModel() + "/" + SeafileLog.getSystemVersion());
    }

    public static void utilsLogInfo(boolean b, String info) {
        if (b) {
            SeafileLog.d(DEBUG_TAG, info);
        } else {
            Log.d(DEBUG_TAG, info);
        }
    }

    public static final String EXCEPTION_TYPE_CRASH = "crash_exception";

    public static void migrateSyncs(Account account) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            SharedPreferences preferences = SeadroidApplication.getAppContext().getSharedPreferences("SeafileSyncPreferences", Context.MODE_PRIVATE);
            boolean hasMigrated = preferences.getBoolean("hasMigrated" + account.getEmail(), false);

            if (!hasMigrated) {
                SettingsManager settingsMgr = SettingsManager.instance();
                SeafSyncSettingsService settingsService = new SeafSyncSettingsService();
                SeafSyncLogsService logService = new SeafSyncLogsService();
                Account accountCamera = new CameraUploadManager(SeadroidApplication.getAppContext()).getCameraAccount();
                String cameraEmail = accountCamera != null ? accountCamera.getEmail() : "";
                String folderEmail = settingsMgr.getBackupEmail();

                if (account.getEmail().equals(cameraEmail)) {
                    migrationMedia(settingsService, logService, account.getEmail());
                }


                if (account.getEmail().equals(folderEmail)) {
                    migrationFolders(account.getEmail(), settingsService, logService);
                }


                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("hasMigrated" + account.getEmail(), true);
                editor.apply();
            }

        });
    }

    private static void migrationMedia(SeafSyncSettingsService settingsService, SeafSyncLogsService logService, String account) {
        SettingsManager settingsMgr = SettingsManager.instance();
        String targetRepoId = settingsMgr.getCameraUploadRepoId();
        List<String> bucketList = settingsMgr.getCameraUploadBucketList();
        List<GalleryBucketUtils.Bucket> selectedBuckets = new ArrayList<>();

        if (targetRepoId != null && !targetRepoId.isEmpty()) {

            List<GalleryBucketUtils.Bucket> allBuckets = GalleryBucketUtils.getMediaBuckets(SeadroidApplication.getAppContext());
            for (GalleryBucketUtils.Bucket bucket : allBuckets) {
                if (bucketList.contains(bucket.id)) {
                    selectedBuckets.add(bucket);
                }
            }

            if (selectedBuckets.isEmpty()) {
                migrationSyncGalleryAndAlbum(settingsService, logService, settingsMgr, targetRepoId, account, allBuckets);
            } else {
                migrationSyncGalleryAndAlbum(settingsService, logService, settingsMgr, targetRepoId, account, selectedBuckets);
            }

        }
    }

    private static void migrationSyncGalleryAndAlbum(SeafSyncSettingsService settingsService, SeafSyncLogsService logService, SettingsManager settingsMgr, String targetRepoId, String account, List<GalleryBucketUtils.Bucket> allBuckets) {
        CameraUploadDBHelper dbHelper = CameraUploadDBHelper.getInstance();
        List<File> uploadedFiles = dbHelper.getUploadedFiles();
        SeafSyncGalleryProvider galleryProvider = new SeafSyncGalleryProvider();
        for (GalleryBucketUtils.Bucket bucket : allBuckets) {

            String albumPath = galleryProvider.getAlbumPath(bucket.id);
            SeafSyncSettings settings = new SeafSyncSettings();
            settings.setType(SeafSyncType.Album);

            if (settingsMgr.isDataPlanAllowed()) {
                settings.setNetwork(SeafSyncNetwork.WifiAndData);
            } else {
                settings.setNetwork(SeafSyncNetwork.Wifi);
            }

            settings.setUploadVideos(settingsMgr.isVideosUploadAllowed());
            settings.setMode(SeafSyncMode.Complete);
            settings.setCreationDate(new Date());
            settings.setAccountId(account);
            settings.setRepoId(targetRepoId);
            settings.setResourceUri(Uri.fromFile(new File(albumPath)));
            settings.setRepoPath("/My Photos/" + bucket.name);
            settingsService.add(settings);

            for (File uploadedFile : uploadedFiles) {
                SeafSyncFileItem item = new SeafSyncFileItem(uploadedFile);

                if (item.getIdentifier() != null) {
                    Date date = new Date();

                    if (item.getCreationDate() == null) {
                        item.setCreationDate(date);
                    }

                    if (item.getModificationDate() == null) {
                        item.setModificationDate(date);
                    }

                    if (albumPath.equals(item.getPath().getPath().replace(item.getPath().getName(), ""))) {
                        logService.log(item, settings);
                        logService.markAsUploaded(item, settings, "");
                    }
                }

            }
        }
    }

    private static void migrationFolders(String account, SeafSyncSettingsService settingsService, SeafSyncLogsService logService) {
        HashMap<String, HashMap<String, SeafSyncSettings>> settingsHashMap = new HashMap<>();

        FolderBackupDBHelper dbHelper = FolderBackupDBHelper.getDatabaseHelper();
        String backupPaths = SettingsManager.instance().getBackupPaths();
        if (!TextUtils.isEmpty(backupPaths)) {
            List<String> pathsList = StringTools.getJsonToList(backupPaths);
            pathsList = getRootPaths(pathsList);
            if (pathsList != null) {
                for (String path : pathsList) {
                    File pathBackup = new File(path);
                    List<FolderBackupInfo> allFiles = new ArrayList<>();
                    for (FolderBackupInfo folder : dbHelper.getAllFolderBackupInfo()) {
                        File fileBackup = new File(folder.filePath);
                        if (fileBackup.getAbsolutePath().startsWith(pathBackup.getAbsolutePath())) {
                            allFiles.add(folder);
                        }
                    }
                    createSyncMigrationFolders(path, allFiles, account, settingsHashMap, settingsService, logService);
                }
            }
        }

//        settingsHashMap.forEach((key, hash) -> {
//            Log.d(DEBUG_TAG, key);
//            hash.forEach((subkey, settings) -> {
//                Log.d(DEBUG_TAG, "\t" + subkey);
//            });
//        });
    }

    public static List<String> getRootPaths(List<String> paths) {
        List<String> rootDirs = new ArrayList<>();
        rootDirs.addAll(paths);

        for (int i = 0; i < paths.size(); i++) {
            for (int j = 0; j < paths.size(); j++) {
                if (i != j && paths.get(j).startsWith(paths.get(i))) {
                    rootDirs.remove(paths.get(j));
                }
            }
        }

        return rootDirs;
    }

    public static boolean isLastLevel(String path, List<String> paths) {
        for (String otherPath : paths) {
            if (!otherPath.equals(path) && otherPath.startsWith(path)) {
                return false;
            }
        }

        return true;
    }

    private static void createSyncMigrationFolders(
            String path,
            List<FolderBackupInfo> allFiles,
            String account,
            HashMap<String, HashMap<String, SeafSyncSettings>> settingsHashMap,
            SeafSyncSettingsService settingsService,
            SeafSyncLogsService logService
    ) {
        SettingsManager settingsMgr = SettingsManager.instance();
        for (FolderBackupInfo folderInfo : allFiles) {
            SeafSyncSettings settings = null;

            if (settingsHashMap.get(folderInfo.repoID) != null) {
                settings = settingsHashMap.get(folderInfo.repoID).get("/" + new File(path).getName());
            }

            if (settings == null) {
                settings = new SeafSyncSettings();
                settings.setType(SeafSyncType.Folder);

                if (settingsMgr.isFolderBackupDataPlanAllowed()) {
                    settings.setNetwork(SeafSyncNetwork.WifiAndData);
                } else {
                    settings.setNetwork(SeafSyncNetwork.Wifi);
                }

                settings.setUploadVideos(settingsMgr.isVideosUploadAllowed());
                settings.setMode(SeafSyncMode.Complete);
                settings.setCreationDate(new Date());
                settings.setAccountId(account);
                settings.setRepoId(folderInfo.repoID);
                settings.setResourceUri(Uri.fromFile(new File(path)));
                settings.setRepoPath("/" + new File(path).getName());
                settingsService.add(settings);


                if (settingsHashMap.get(folderInfo.repoID) == null) {
                    settingsHashMap.put(folderInfo.repoID, new HashMap<>());
                }

                settingsHashMap.get(folderInfo.repoID).put("/" + new File(path).getName(), settings);
            }

            SeafSyncFileItem item = new SeafSyncFileItem(new File(folderInfo.filePath));

            if (item.getIdentifier() != null) {
                Date date = new Date();

                if (item.getCreationDate() == null) {
                    item.setCreationDate(date);
                }

                if (item.getModificationDate() == null) {
                    item.setModificationDate(date);
                }

                if (!item.isDirectory()) {
                    logService.log(item, settings);
                    logService.markAsUploaded(item, settings, "");
                }
            }
        }
    }

    public static Date addTimeToDate(Date date, long miliseconds) {
        long timeMiliseconds = date.getTime();
        timeMiliseconds += miliseconds;
        return new Date(timeMiliseconds);
    }

    public static void openWebPlans(Activity activity){
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getResources().getString(R.string.change_plan_url)));
        activity.startActivity(intent);
    }

    public static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf('/');
        return filename.substring(index + 1);
    }

    public static String getFileName(@NonNull Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        String filename = null;

        if (mimeType == null && context != null) {
            String path = null;
            try {
                path = getPath(context, uri);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            if (path == null) {
                filename = getName(uri.toString());
            } else {
                File file = new File(path);
                filename = file.getName();
            }
        } else {
            Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }
        }

        return filename;
    }

    public static File getDocumentCacheDir(Context context) {
        File dir = new File(context.getCacheDir(), DOCUMENTS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Nullable
    public static File generateFileName(String name, File directory) {
        if (name == null) {
            return null;
        }
        File file = new File(directory, name);
        if (file.exists()) {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);
            }
            int index = 0;
            while (file.exists()) {
                index++;
                file = new File(directory, fileName + '(' + index + ')' + extension);
            }
        }
        try {
            if (!file.createNewFile()) {
                return null;
            }
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    public static String getPathFromUri(final Context context, final Uri uri) {
        String docId;
        Uri contentUri;
        String path = null;
        String[] split;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                if ("primary".equalsIgnoreCase(DocumentsContract.getDocumentId(uri).split(":")[0])) {
                    split = Objects.requireNonNull(uri.getPath()).split(":");
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                if (id != null && id.startsWith("raw:")) {
                    return id.substring(4);
                }
                if (id != null && id.startsWith("msf:")) {
                    return id.substring(4);
                }
                String[] contentUriPrefixesToTry = {"content://downloads/public_downloads", "content://downloads/my_downloads"};
                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    Uri contentUri2 = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id).longValue());
                    try {
                        path = getDataColumn(context, contentUri2, null, null);
                    } catch (Exception e) {
                    }
                    if (path != null) {
                        return path;
                    }
                }
                String fileName = getFileName(context, uri);
                File cacheDir = getDocumentCacheDir(context);
                File file = generateFileName(fileName, cacheDir);
                if (file == null) {
                    return null;
                }
                String destinationPath = file.getAbsolutePath();
                saveFileFromUri(context, uri, destinationPath);
                return destinationPath;
            } else if (isMediaDocument(uri) && (docId = DocumentsContract.getDocumentId(uri)) != null && docId.contains(":")) {
                return uri.toString();
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri) || isGoogleDriveUri(uri)) {
                return uri.getLastPathSegment();
            }
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            is.read(buf);
            do {
                bos.write(buf);
            } while (is.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.contentprovider".equals(uri.getAuthority());
    }

    public static boolean isGoogleDriveUri(Uri uri) {
        return "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
    }
}
