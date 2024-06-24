package com.ateneacloud.drive.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.ateneacloud.drive.R;

public class RequestStorageDialog {

    private IRequestStorageDialog irequestStorageDialog;

    private Context context;

    private RequestStorageDialog(){}

    private RequestStorageDialog(Context context) {
        this.context = context;
    }

    public static RequestStorageDialog build(Context context){
        return new RequestStorageDialog(context);
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.storage_access))
                .setMessage(context.getResources().getString(R.string.storage_access_request))
                .setPositiveButton(context.getResources().getString(R.string.allow), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(irequestStorageDialog != null){
                            irequestStorageDialog.acceptRequestStorage();
                        }

                    }
                })
                .setNegativeButton(context.getResources().getString(R.string.deny), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(irequestStorageDialog != null){
                            irequestStorageDialog.denieRequestStorage();
                        }
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if(irequestStorageDialog != null){
                            irequestStorageDialog.denieRequestStorage();
                        }
                    }
                })

                .show();
    }

    public RequestStorageDialog setIRequestStorageDialog(IRequestStorageDialog irequestStorageDialog) {
        this.irequestStorageDialog = irequestStorageDialog;
        return this;
    }

    public interface IRequestStorageDialog {
        public void acceptRequestStorage();

        public void denieRequestStorage();
    }

}
