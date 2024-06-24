package com.ateneacloud.drive.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.ateneacloud.drive.R;

public class ContinueDialog {

    private ContinueDialogContinueTask continueDialogContinueTask;

    private Context context;

    public final static int UPLOAD_FILES = 0, DOWNLOAD_FILES = 1;

    private int type;

    private ContinueDialog() {
    }

    private ContinueDialog(Context context, int type) {
        this.context = context;
        this.type = type;
    }

    public static ContinueDialog build(Context context, int type) {
        return new ContinueDialog(context, type);
    }

    public void show() {
        String title = "", text = "";

        if (type == DOWNLOAD_FILES) {
            title = context.getResources().getString(R.string.file_download);
            text = context.getResources().getString(R.string.continue_downloading_files);
        } else {
            title = context.getResources().getString(R.string.file_upload);
            text = context.getResources().getString(R.string.continue_uploading_files);
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(text)
                .setPositiveButton(context.getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (continueDialogContinueTask != null) {
                            continueDialogContinueTask.continueTask();
                        }

                    }
                }).setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (continueDialogContinueTask != null) {
                            continueDialogContinueTask.cancelTask();
                        }
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        if (continueDialogContinueTask != null) {
                            continueDialogContinueTask.cancelTask();
                        }
                    }
                }).show();
    }

    public ContinueDialog setOnContinueTask(ContinueDialogContinueTask continueDialogContinueTask) {
        this.continueDialogContinueTask = continueDialogContinueTask;
        return this;
    }

    public interface ContinueDialogContinueTask {
        public void continueTask();

        public void cancelTask();
    }
}
