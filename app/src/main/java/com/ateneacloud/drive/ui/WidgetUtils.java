package com.ateneacloud.drive.ui;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import android.text.ClipboardManager;
import android.webkit.MimeTypeMap;

import com.blankj.utilcode.util.ToastUtils;
import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountInfo;
import com.ateneacloud.drive.ui.activity.BaseActivity;
import com.ateneacloud.drive.ui.activity.BrowserActivity;
import com.ateneacloud.drive.ui.activity.GalleryActivity;
import com.ateneacloud.drive.ui.activity.MarkdownActivity;
import com.ateneacloud.drive.ui.dialog.AppChoiceDialog;
import com.ateneacloud.drive.ui.dialog.GetShareLinkDialog;
import com.ateneacloud.drive.ui.dialog.GetShareLinkEncryptDialog;
import com.ateneacloud.drive.ui.dialog.TaskDialog;
import com.ateneacloud.drive.ui.dialog.ChangePlanDialog;
import com.ateneacloud.drive.util.FileExports;
import com.ateneacloud.drive.util.FileExtension;
import com.ateneacloud.drive.util.Utils;
import com.ateneacloud.drive.util.ValidatesFiles;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Random;

/**
 * Activity Utils
 */
public class WidgetUtils {
    public static final String MIME_ANDROID = "application/vnd.android.package-archive";

    public static void chooseShareApp(final BaseActivity activity,
                                      final String repoID,
                                      final String path,
                                      final boolean isdir,
                                      final Account account,
                                      final String password,
                                      final String days) {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        // Get a list of apps
        List<ResolveInfo> infos = Utils.getAppsByIntent(shareIntent);

        String title = activity.getString(isdir ? R.string.share_dir_link : R.string.share_file_link);

        AppChoiceDialog dialog = new AppChoiceDialog();
        dialog.addCustomAction(0, activity.getResources().getDrawable(R.drawable.copy_link),
                activity.getString(R.string.copy_link));
        dialog.init(title, infos, new AppChoiceDialog.OnItemSelectedListener() {
            @Override
            public void onCustomActionSelected(AppChoiceDialog.CustomAction action) {
                final GetShareLinkDialog gdialog = new GetShareLinkDialog();
                gdialog.init(repoID, path, isdir, account, password, days);
                gdialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
                    @Override
                    @SuppressWarnings("deprecation")
                    public void onTaskSuccess() {
                        ClipboardManager clipboard = (ClipboardManager)
                                activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(gdialog.getLink());
                        // ClipData clip = ClipData.newPlainText("seafile shared link", gdialog.getLink());
                        // clipboard.setPrimaryClip(clip);
                        ToastUtils.showLong(R.string.link_ready_to_be_pasted);
                    }

                    @Override
                    public void onTaskFailed(SeafException e) {
                        super.onTaskFailed(e);
                        gdialog.dismiss();
                        if (e.getCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                            ToastUtils.showLong(R.string.share_link_no_permission);
                        } else {
                            ToastUtils.showLong(e.getMessage());
                        }
                    }
                });
                gdialog.show(activity.getSupportFragmentManager(), "DialogFragment");
            }

            @Override
            public void onAppSelected(ResolveInfo appInfo) {
                String className = appInfo.activityInfo.name;
                String packageName = appInfo.activityInfo.packageName;
                shareIntent.setClassName(packageName, className);

                final GetShareLinkDialog gdialog = new GetShareLinkDialog();
                gdialog.init(repoID, path, isdir, account, password, days);
                gdialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
                    @Override
                    public void onTaskSuccess() {
                        shareIntent.putExtra(Intent.EXTRA_TEXT, gdialog.getLink());
                        activity.startActivity(shareIntent);
                    }
                });
                gdialog.show(activity.getSupportFragmentManager(), "DialogFragment");
            }

        });
        dialog.show(activity.getSupportFragmentManager(), BrowserActivity.CHOOSE_APP_DIALOG_FRAGMENT_TAG);
    }

    public static void inputSharePassword(final BaseActivity activity,
                                          final String repoID,
                                          final String path,
                                          final boolean isdir,
                                          final Account account) {
        final GetShareLinkEncryptDialog dialog = new GetShareLinkEncryptDialog();
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                String password = dialog.getPassword();
                String days = dialog.getDays();
                chooseShareApp(activity, repoID, path, isdir, account, password, days);
            }
        });
        dialog.show(activity.getSupportFragmentManager(), BrowserActivity.CHARE_LINK_PASSWORD_FRAGMENT_TAG);

    }

    /**
     * if dir will share dir link .
     * if local file ,will share file to wachat app.
     * if server file , it will download file and share file.
     *
     * @param activity
     * @param account
     * @param repoID
     * @param path
     * @param fileName
     * @param fileSize
     * @param isdir
     */
    public static void ShareWeChat(final BaseActivity activity, Account account, String repoID, String path,
                                   String fileName,
                                   long fileSize,
                                   boolean isdir) {

        if (isdir) {//share  link
            final Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            ResolveInfo weChatInfo = Utils.getWeChatIntent(shareIntent);
            if (weChatInfo == null) {
                activity.showShortToast(activity, R.string.no_app_available);
                return;
            }
            String className = weChatInfo.activityInfo.name;
            String packageName = weChatInfo.activityInfo.packageName;
            shareIntent.setClassName(packageName, className);
            final GetShareLinkDialog gdialog = new GetShareLinkDialog();
            gdialog.init(repoID, path, isdir, account, null, null);
            gdialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
                @Override
                public void onTaskSuccess() {
                    shareIntent.putExtra(Intent.EXTRA_TEXT, gdialog.getLink());
                    activity.startActivity(shareIntent);
                }
            });
            gdialog.show(activity.getSupportFragmentManager(), "DialogFragment");
        } else {//share  files
            BrowserActivity browserActivity = ((BrowserActivity) activity);
            String repoName = ((BrowserActivity) activity).getNavContext().getRepoName();
            String dirPath = ((BrowserActivity) activity).getNavContext().getDirPath();

            String fullPath = Utils.pathJoin(dirPath, fileName);
            final File file = browserActivity.getDataManager().getLocalRepoFile(repoName, repoID, fullPath);
            Uri uri = null;
            if (android.os.Build.VERSION.SDK_INT > 23) {
                uri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName(), file);
            } else {
                uri = Uri.fromFile(file);
            }
            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType(Utils.getFileMimeType(file));
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            ResolveInfo weChatInfo = Utils.getWeChatIntent(sendIntent);
            if (weChatInfo == null) {
                activity.showShortToast(activity, R.string.no_app_available);
                return;
            }
            String className = weChatInfo.activityInfo.name;
            String packageName = weChatInfo.activityInfo.packageName;
            sendIntent.setClassName(packageName, className);
            if (!Utils.isNetworkOn() && file.exists()) {
                activity.startActivity(sendIntent);
                return;
            }
            browserActivity.fetchFileAndExport(weChatInfo, sendIntent, repoName, repoID, path, fileSize);

        }
    }

    public static void showFile(final BaseActivity activity, File file, String repoID, String repoName, String repoDir, boolean onlyRead) {
        showFile(activity, file, repoID, repoName, repoDir, false, onlyRead);
    }

    /**
     * display the file according to its file type
     *
     * @param file
     */
    public static void showFile(final BaseActivity activity, File file, String repoID, String repoName, String repoDir, boolean isOpenWith, boolean isOnlyRead) {

        String name = file.getName();
        String suffix = name.substring(name.lastIndexOf('.') + 1).toLowerCase();

        //Open markdown and txt files in MarkdownActivity
        boolean isTextMime = Utils.isTextMimeType(name);
        if (isTextMime && !isOpenWith) {
            startMarkdownActivity(activity, file.getPath(),isOnlyRead, repoID, repoName, repoDir);
            activity.overridePendingTransition(0, 0);
            return;
        }

        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (mime == null && isTextMime) {
            mime = "text/*"; // set .md  .markdown .txt file type//
        } else if (mime == null) {
            mime = "*/*"; // forces app chooser dialog on unknown type//
        }

        if (MIME_ANDROID.equals(mime)) {
//            showFileForAndroid(activity,file,name);
            return;
        }

        Intent open = new Intent(Intent.ACTION_VIEW);
        open.addFlags(FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT > 23) {
            Uri photoURI = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName(), file);
            open.setDataAndType(photoURI, mime);
            open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            open.setDataAndType((Uri.fromFile(file)), mime);
        }
        if (Build.VERSION.SDK_INT < 30) {
            if (activity.getPackageManager().resolveActivity(open, 0) == null) {
                String message = String.format(activity.getString(R.string.op_exception_suitable_app_not_found), mime);
                activity.showShortToast(activity, message);
                mime = "*/*";
                open.setType(mime);
            }
        }

        try {
            activity.startActivity(open);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void showFileForAndroid(final BaseActivity activity, File file, String fileName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver contentResolver = activity.getContentResolver();
                FileExports.exportFileAndroid10AndAbove(fileName, MIME_ANDROID, contentResolver, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int nId = new Random(10000).nextInt();
        String channelName = "seadroid-downloader";

        NotificationManager manager = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(channelName, channelName, NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);


        Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(activity, nId, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(activity, channelName)
                .setContentTitle(fileName + " " + activity.getString(R.string.download_finished))
                .setContentText(activity.getString(R.string.open))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        manager.notify(nId, notification);
    }

    public static void showRepo(Context context, String repoID, String repoName, String path, String dirID) {
        Intent intent = new Intent(context, BrowserActivity.class);
        intent.putExtra("repoID", repoID);
        intent.putExtra("repoName", repoName);
        intent.putExtra("path", path);
        intent.putExtra("dirID", dirID);
        context.startActivity(intent);
    }

    public static void showStarredRepo(Activity activity, String repoID, String repoName, String path, String dirID) {
        Intent intent = new Intent(activity, BrowserActivity.class);
        intent.putExtra("repoID", repoID);
        intent.putExtra("repoName", repoName);
        intent.putExtra("path", path);
        intent.putExtra("dirID", dirID);
        activity.startActivityForResult(intent, 0);
    }

    public static void startMarkdownActivity(Context context, String path, boolean isOnlyRead, String repoID, String repoName, String repoDir) {
        Intent intent = new Intent(context, MarkdownActivity.class);
        intent.putExtra("repoID", repoID);
        intent.putExtra("repoName", repoName);
        intent.putExtra("repoDir", repoDir);
        intent.putExtra("path", path);
        intent.putExtra("only_read", isOnlyRead);
        context.startActivity(intent);
    }

    /**
     * start and pass data to {@link GalleryActivity}
     *
     * @param repoId
     * @param path
     * @param fileName
     * @param account
     */
    public static void startGalleryActivity(Activity activity, String repoName, String repoId, String path, String fileName, Account account, AccountInfo accountInfo) {

        String extension = FileExtension.getExtensionFromUri(activity,Uri.fromFile(new File(fileName)));
        if (accountInfo != null && ValidatesFiles.isValidType(accountInfo, extension)) {
            Intent intent = new Intent(activity, GalleryActivity.class);
            intent.putExtra("repoName", repoName);
            intent.putExtra("repoId", repoId);
            intent.putExtra("path", path);
            intent.putExtra("account", account);
            intent.putExtra("fileName", fileName);
            activity.startActivity(intent);
        } else {
            try {
                ChangePlanDialog
                        .build(activity, ChangePlanDialog.NOT_ALLOWED_IMAGE)
                        .setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                            @Override
                            public void changePlanDialogResponseYes() {
                                Utils.openWebPlans(activity);
                            }

                            @Override
                            public void changePlanDialogResponseNo() {

                            }

                            @Override
                            public void changePlanDialogResponseNeither() {

                            }
                        }).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static int getThumbnailWidth() {
        return (int) SeadroidApplication.getAppContext().getResources().getDimension(R.dimen.lv_icon_width);
    }
}
