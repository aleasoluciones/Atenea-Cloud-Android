package com.ateneacloud.drive.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.ateneacloud.drive.R;

public class StepRecycleBin {

    private StepRecycleBin.ReplaceTrashTask replaceTrashTask;
    private Context context;
    private int type;
    public static final int MULTIPLE_FILES_IN_TRASH = 0, SINGLE_FILE_IN_TRASH = 1;
    private StepRecycleBin() {}

    private StepRecycleBin(Context context, int type) {
        this.context = context;
        this.type = type;
    }

    public static StepRecycleBin build(Context context) {
        return new StepRecycleBin(context, SINGLE_FILE_IN_TRASH);
    }

    public static StepRecycleBin build(Context context, int type) {
        return new StepRecycleBin(context, type);
    }

    public void show() {
        String title = context.getResources().getString(R.string.upload_replace), text = type == SINGLE_FILE_IN_TRASH ? context.getResources().getString(R.string.exists_file_in_recycle_bin) : context.getResources().getString(R.string.exists_files_in_recycle_bin);


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(text)
                .setPositiveButton(context.getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (replaceTrashTask != null) {
                            replaceTrashTask.continueTask();
                        }

                    }
                }).setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (replaceTrashTask != null) {
                            replaceTrashTask.cancelTask();
                        }
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        if (replaceTrashTask != null) {
                            replaceTrashTask.cancelTask();
                        }
                    }
                }).show();
    }

    public StepRecycleBin setOnReplaceTrash(StepRecycleBin.ReplaceTrashTask replaceTrashTask) {
        this.replaceTrashTask = replaceTrashTask;
        return this;
    }

    public interface ReplaceTrashTask {
        public void continueTask();

        public void cancelTask();
    }

}
