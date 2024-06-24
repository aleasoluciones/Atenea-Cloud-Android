package com.ateneacloud.drive.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.ateneacloud.drive.R;

public class DeleteSyncDialog {
    private DeleteSyncDialogResponse deleteSyncDialogResponse;

    private Context context;

    private DeleteSyncDialog(Context context) {
        this.context = context;
    }

    public static DeleteSyncDialog build(Context context) {
        return new DeleteSyncDialog(context);
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.confirm))
                .setMessage(context.getResources().getString(R.string.delete_sync_configuration))
                .setPositiveButton(context.getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (deleteSyncDialogResponse != null) {
                            deleteSyncDialogResponse.deleteSynchronization();
                        }
                    }
                })
                .setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public DeleteSyncDialog setResponse(DeleteSyncDialogResponse deleteSyncDialogResponse) {
        this.deleteSyncDialogResponse = deleteSyncDialogResponse;
        return this;
    }

    public interface DeleteSyncDialogResponse {
        public void deleteSynchronization();
    }

}
